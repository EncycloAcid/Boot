import Foundation

/// A simple scripted player. It plays sensibly rather than well, which makes it a
/// good yardstick for balance runs and a smoke test for the whole simulation.
public struct Autopilot: Sendable {
    /// Lanes to move into at the Pivot Point and later expansions, in order of preference.
    public var lanePreference: [Lane]
    /// Sell outright when a buyout arrives after the Pivot Point.
    public var sellsOnOffer: Bool
    /// How hard to push combo dials.
    public var comboIntensity: Double

    public init(lanePreference: [Lane] = [.apps, .hardware, .ai, .social, .games, .websites],
                sellsOnOffer: Bool = false, comboIntensity: Double = 0.5) {
        self.lanePreference = lanePreference
        self.sellsOnOffer = sellsOnOffer
        self.comboIntensity = comboIntensity
    }

    /// Make this week's decisions, then advance one week.
    public func playWeek(_ s: inout GameState) {
        guard !s.isOver else { return }
        var guardCount = 0
        while s.needsDecision && !s.isOver && guardCount < 20 {
            guardCount += 1
            if s.pendingPivot != nil { resolvePivot(&s) } else { resolveEvent(&s) }
        }
        guard !s.isOver else { return }
        manage(&s)
        try? s.advance()
    }

    /// Play until the game ends. Returns the ending.
    @discardableResult
    public func playToEnd(_ s: inout GameState) -> EndingReport? {
        while !s.isOver && s.week <= GameState.lengthWeeks + 5 {
            playWeek(&s)
        }
        return s.ending
    }

    // MARK: Decisions

    private func resolveEvent(_ s: inout GameState) {
        guard let e = s.events.first else { return }
        let cashy = s.cash > 26 * s.weeklyBurn
        let choice: Int = switch e.kind {
        case .poach: cashy ? 0 : 1
        case .buyout: sellsOnOffer ? 0 : 1
        case .boardDemand: 0
        case .recall: 0
        case .componentShortage: cashy ? 0 : 1
        case .moderationFire: cashy ? 0 : 1
        case .pressScandal: 0
        case .botWave: 0
        case .regulatorInquiry: s.cash > 3_000_000 ? 0 : 1
        case .viralMoment: 0
        case .burnoutWarning: 0
        case let .comboProblem(c): s.cash > s.comboFixCost(c) * 4 ? 0 : 1
        case .ecosystemAntitrust: 0
        }
        try? s.choose(choice)
    }

    private func resolvePivot(_ s: inout GameState) {
        guard let offer = s.pendingPivot else { return }
        let mode: PivotMode = s.founder.energy > 55 && s.trailingProfit(weeks: 13) > 0 ? .expand : .pivot
        var options = lanePreference.filter { !s.lanes.contains($0) }
        if let suggested = offer.suggestedLane, let i = options.firstIndex(of: suggested) {
            options.remove(at: i)
            options.insert(suggested, at: 0)
        }
        for lane in options {
            for venture in [false, true] where s.pivotBlocker(lane: lane, mode: mode, fundWithVenture: venture) == nil {
                try? s.resolvePivot(to: lane, mode: mode, fundWithVenture: venture)
                return
            }
        }
        // Fall back to whatever works.
        for lane in s.pivotChoices {
            for m in PivotMode.allCases {
                for venture in [false, true] where s.pivotBlocker(lane: lane, mode: m, fundWithVenture: venture) == nil {
                    try? s.resolvePivot(to: lane, mode: m, fundWithVenture: venture)
                    return
                }
            }
        }
    }

    // MARK: Running the company

    private func manage(_ s: inout GameState) {
        if s.crunch { try? s.setCrunch(false) }
        if s.founder.energy < 35 && !s.founder.isOnVacation { s.takeVacation() }
        if s.ipoBlocker == nil { try? s.goPublic(); return }

        if s.runwayWeeks < 20, case .success = s.roundOffer() {
            try? s.raiseRound()
        }
        let lean = s.runwayWeeks < 16 && s.staff.isEmpty
        if lean != s.founder.freelancing { try? s.setFreelancing(lean) }

        for lane in s.lanes {
            switch lane {
            case .games: manageGames(&s)
            case .websites: manageWeb(&s)
            case .apps: manageApps(&s)
            case .social: manageSocial(&s)
            case .hardware: manageHardware(&s)
            case .ai: manageAI(&s)
            }
        }

        for c in s.crossovers where !c.built && c.buildWeeksLeft == c.combo.buildWeeks {
            if s.cash > c.combo.buildCost * 5 && s.trailingProfit(weeks: 13) > 0 {
                try? s.buildCrossover(c.combo)
                try? s.setIntensity(c.combo, comboIntensity)
            }
        }

        if s.expansionBlocker == nil, let next = lanePreference.first(where: { !s.lanes.contains($0) }) {
            if s.cash > next.selfFundThreshold + next.setupCost {
                try? s.expand(into: next)
            } else if next.isCapitalIntensive, case .success = s.ventureForLane(next) {
                try? s.expand(into: next, fundWithVenture: true)
            }
        }

        hire(&s)
    }

    /// Rough headcount where each lane stops benefiting from more people.
    private func usefulHeadcount(_ lane: Lane, _ s: GameState) -> Int {
        switch lane {
        case .games: s.cash > 5_000_000 ? 35 : 12
        case .websites: 3 + (s.web?.active.count ?? 0) * 2
        case .apps: 3 + Int((s.apps?.users ?? 0) / 150_000)
        case .social: 3 + Int((s.social?.users ?? 0) / 1_000_000)
        case .hardware: 25
        case .ai: 20
        }
    }

    private func hire(_ s: inout GameState) {
        guard s.week % 2 == 0 else { return }
        let profit = s.trailingProfit(weeks: 13) / 13
        let budget = max(profit * 0.5, s.venture.isBacked ? s.cash / 300 : 0)
        guard budget > 0, s.cash > 40 * (s.payroll + 2_000) else { return }
        let open = s.lanes.filter { s.staff(in: $0).count < usefulHeadcount($0, s) }
        let needs = open.min { s.staff(in: $0).count < s.staff(in: $1).count }
        guard let lane = needs else { return }
        let fit = s.candidates
            .filter { lane.weight(for: $0.role) >= 0.8 && $0.askingSalary <= budget }
            .max { Double($0.skill) / $0.askingSalary < Double($1.skill) / $1.askingSalary }
        if let c = fit { try? s.hire(c.id, lane: lane) }
    }

    private func manageGames(_ s: inout GameState) {
        guard let g = s.games else { return }
        if let p = g.project {
            if p.isReady && (p.polish >= p.scope.work * 1.3 || p.weeksInDev > 40 || s.runwayWeeks < 10) {
                try? s.launchGame()
            }
            return
        }
        let output = s.staffing(for: .games).output
        let scope: GameScope = output > 80 && s.cash > 5_000_000 ? .large : output > 14 && s.cash > 200_000 ? .medium : .small
        try? s.startGame(scope)
    }

    private func manageWeb(_ s: inout GameState) {
        guard let w = s.web else { return }
        let capacity = max(1, Int(s.staffing(for: .websites).output / 5))
        for offer in w.offers.sorted(by: { $0.value / $0.work > $1.value / $1.work }) where (s.web?.active.count ?? 0) < capacity {
            try? s.acceptContract(offer.id)
        }
    }

    private func manageApps(_ s: inout GameState) {
        guard let a = s.apps, a.isLive else { return }
        if a.quality > 72 && a.monetization == .ads && a.users > 20_000 {
            try? s.setMonetization(.subscription)
        }
    }

    private func manageSocial(_ s: inout GameState) {
        guard let so = s.social else { return }
        try? s.setModeration((0.25 + so.toxicity + so.botShare).clamped(0.2, 0.9))
        try? s.setAdLoad(so.users > 2_000_000 ? 0.5 : so.users > 200_000 ? 0.25 : 0.1)
    }

    private func manageHardware(_ s: inout GameState) {
        guard let h = s.hardware else { return }
        let inbound = h.orders.reduce(0) { $0 + $1.units }
        if h.design == nil {
            let age = h.onMarket.map { s.week - $0.launchWeek } ?? 999
            if age > 40 {
                let flagship = s.cash > 4_000_000
                try? s.startDevice(flagship ? .phone : .headphones, flagship ? .flagship : .budget)
            }
        }
        if let d = s.hardware?.design {
            if d.completion > 0.55 && d.preorderedStock == 0 && inbound == 0 {
                let spec = d.spec
                let awareness = s.hardware?.brandAwareness ?? 0.2
                let estimate = spec.category.baseDemand * spec.tier.demandMultiplier * awareness * 14
                let affordable = s.cash * 0.45 / spec.unitCost
                let units = Int(min(estimate, affordable))
                if units > 100 { try? s.orderUnits(units) }
            }
            if d.isReady && (d.preorderedStock > 0 || inbound == 0) {
                try? s.launchDevice()
            }
        }
        if let h = s.hardware, let m = h.onMarket, h.design == nil, inbound == 0,
           Double(h.inventory) < h.lastDemand * 3, s.week - m.launchWeek < 30 {
            let units = Int(min(h.lastDemand * 10, s.cash * 0.35 / m.spec.unitCost))
            if units > 100 { try? s.orderUnits(units) }
        }
    }

    private func manageAI(_ s: inout GameState) {
        guard let lab = s.ai else { return }
        if lab.training == nil {
            let wanted = TrainingSize.allCases.first { lab.capability < $0.ceiling * 0.8 } ?? .frontier
            let budget: [TrainingSize: Double] = [.small: 400_000, .medium: 2_500_000, .frontier: 14_000_000]
            let size: TrainingSize? = s.cash > budget[wanted]! ? wanted : nil
            if let size { try? s.startTraining(size) }
        }
        if s.cash > 30_000_000 && lab.gpusOwned < 16 { try? s.buyGPUs(4) }
    }
}
