import Foundation

extension GameState {
    /// Simulate one week.
    public mutating func advance() throws {
        guard ending == nil else { throw ActionError.gameOver }
        guard !needsDecision else { throw ActionError.decisionPending }

        week += 1
        ledger.append(WeekLedger(week: week))
        if ledger.count > Self.lengthWeeks + 10 { ledger.removeFirst() }

        updateEra()
        updateRivals()

        // Combos first: they shape every lane's week.
        var fx = ComboEffects()
        for c in crossovers { c.apply(to: &fx, state: self) }
        if let w = web { web!.clientWariness = max(fx.webWariness, w.clientWariness * 0.98) }

        for lane in lanes {
            let ctx = context(for: lane, effects: fx)
            let report: LaneReport
            switch lane {
            case .games: report = games!.tick(ctx, rng: &rng)
            case .websites: report = web!.tick(ctx, rng: &rng)
            case .apps: report = apps!.tick(ctx, rng: &rng)
            case .social: report = social!.tick(ctx, rng: &rng)
            case .hardware: report = hardware!.tick(ctx, rng: &rng)
            case .ai: report = ai!.tick(ctx, rng: &rng)
            }
            apply(report, lane: lane)
            let lanePayroll = staff(in: lane).reduce(0) { $0 + $1.salary }
            recordCost(lanePayroll, lane: lane)
            cash -= lanePayroll
        }

        // Unassigned staff still get paid.
        let idle = staff.filter { $0.lane == nil }.reduce(0) { $0 + $1.salary }
        spend(idle + headquarters.rent, lane: nil)

        updateCrossovers(fx)

        if founder.freelancing && !founder.isOnVacation {
            let gig = founder.freelanceIncome(week: week)
            cash += gig
            recordRevenue(gig, lane: nil)
        }

        stats.peakCash = max(stats.peakCash, cash)
        stats.peakValuation = max(stats.peakValuation, valuation)
        ledger[ledger.count - 1].cash = cash

        updateFounder(fx)
        updateStaff(fx)
        reputation.weeklyDrift()
        if week % 4 == 0 { refreshTalentPool() }
        candidates.removeAll { $0.contestedBy != nil && $0.expiresWeek < week }

        rollWorldEvents()
        boardReview()
        checkPivotPoint()
        updateStreaks()
        checkEndings()
    }

    /// Advance until a decision is needed, the game ends, or `weeks` pass.
    @discardableResult
    public mutating func advance(weeks: Int) throws -> Int {
        var n = 0
        while n < weeks, !needsDecision, ending == nil {
            try advance()
            n += 1
        }
        return n
    }

    private mutating func apply(_ report: LaneReport, lane: Lane) {
        cash += report.revenue - report.expenses
        recordRevenue(report.revenue, lane: lane)
        recordCost(report.expenses, lane: lane)
        handle(report.incidents, lane: lane)
    }

    mutating func handle(_ incidents: [Incident], lane: Lane?) {
        for incident in incidents {
            switch incident {
            case let .log(text, tone): addLog(text, tone, lane: lane)
            case let .reputation(delta): reputation.adjust(delta)
            case let .scar(kind, severity, note): reputation.scar(kind, severity: severity, week: week, note: note)
            case let .event(kind): enqueue(kind)
            case let .stat(key):
                switch key {
                case .hit: stats.hits += 1
                case .flop: stats.flops += 1
                case .recall: stats.recalls += 1
                case .scandal: stats.scandals += 1
                case .release:
                    stats.releases += 1
                    stats.releasesThisQuarter += 1
                case .soldOut: stats.soldOuts += 1
                case .overstock: stats.overstocks += 1
                }
            }
        }
    }

    // MARK: World

    private mutating func updateEra() {
        guard week - world.eraStartWeek >= world.eraLength else { return }
        let unseen = Era.allCases.filter { $0 != .bootstrapYears && !world.pastEras.contains($0) && $0 != world.era }
        let next = unseen.isEmpty ? rng.pick(Era.allCases.filter { $0 != world.era && $0 != .bootstrapYears }) : rng.pick(unseen)
        world.pastEras.append(world.era)
        world.era = next
        world.eraStartWeek = week
        world.eraLength = rng.int(78...130)
        if !stats.erasSeen.contains(next) { stats.erasSeen.append(next) }
        let mine = lanes.map { next.demand($0) }
        let tone: LogTone = mine.contains { $0 > 1.05 } ? .good : mine.contains { $0 < 0.95 } ? .bad : .neutral
        addLog("A new era begins: \(next.name). \(next.blurb)", tone)
    }

    private mutating func updateRivals() {
        for i in world.rivals.indices {
            let r = world.rivals[i]
            let target = (0.5 * world.era.demand(r.lane)).clamped(0.2, 0.9)
            world.rivals[i].strength = (r.strength + (target - r.strength) * 0.02 + rng.normal(sd: 0.01)).clamped(0.05, 1)

            if lanes.contains(r.lane) && rng.chance(0.008) {
                world.rivals[i].strength = min(1, world.rivals[i].strength + 0.1)
                let move = "\(r.name) launched something big in \(r.lane.name.lowercased()). Your market just got tougher."
                world.rivals[i].lastMove = move
                addLog(move, .bad, lane: r.lane)
            }

            // Rivals compete for the same talent.
            let targets = staff.filter { $0.lane == r.lane && $0.skill >= 6 }
            if let best = targets.max(by: { $0.skill < $1.skill }), rng.chance(r.aggression * 0.006) {
                enqueue(.poach(employeeID: best.id, rival: r.name))
            }
        }
    }

    private mutating func rollWorldEvents() {
        for lane in lanes where rng.chance(0.0025) && hasLiveProduct(lane) {
            enqueue(.viralMoment(lane))
        }
        if pivot != nil && valuation >= 2_000_000 && rng.chance(0.005) {
            let bidder = rng.chance(0.5) ? rng.pick(world.rivals).name : rng.pick(["MegaCorp Holdings", "Blackstone-ish Capital", "Conglomo"])
            enqueue(.buyout(bidder: bidder, price: (valuation * rng.range(1.1, 1.6)).rounded()))
        }
        if founder.energy < 20 && !stats.warnedBurnout {
            stats.warnedBurnout = true
            enqueue(.burnoutWarning)
        } else if founder.energy > 40 {
            stats.warnedBurnout = false
        }
    }

    private func hasLiveProduct(_ lane: Lane) -> Bool {
        switch lane {
        case .games: !(games?.releases.isEmpty ?? true)
        case .websites: (web?.completed ?? 0) > 0
        case .apps: apps?.isLive ?? false
        case .social: social?.isLive ?? false
        case .hardware: hardware?.onMarket != nil
        case .ai: (ai?.capability ?? 0) > 0
        }
    }

    // MARK: Combos

    private mutating func updateCrossovers(_ fx: ComboEffects) {
        recordRevenue(fx.revenue, lane: nil)
        cash += fx.revenue
        spend(fx.cost, lane: nil)

        for i in crossovers.indices {
            if !crossovers[i].built && crossovers[i].buildWeeksLeft < crossovers[i].combo.buildWeeks {
                crossovers[i].buildWeeksLeft -= 1
                if crossovers[i].buildWeeksLeft <= 0 {
                    crossovers[i].built = true
                    addLog("\(crossovers[i].combo.product) is live. \(crossovers[i].combo.pitch)", .milestone)
                }
            }
            crossovers[i].pressure += crossovers[i].pressureRate(state: self)
            if crossovers[i].pressure >= 1 {
                crossovers[i].pressure = 0.5
                enqueue(.comboProblem(crossovers[i].combo))
            }
        }

        if isEcosystem {
            stats.ecosystemPressure += stats.ecosystemOpened ? 0.002 : 0.006
            if stats.ecosystemPressure >= 1 { enqueue(.ecosystemAntitrust) }
        }
    }

    // MARK: People

    private mutating func updateFounder(_ fx: ComboEffects) {
        if founder.isOnVacation {
            founder.vacationWeeksLeft -= 1
            founder.energy = min(100, founder.energy + 12)
        } else {
            var delta = 0.6
            if crunch {
                delta -= 3.5
                stats.crunchWeeks += 1
            }
            delta -= 1.6 * Double(max(0, lanes.count - 1))
            delta -= fx.energyDrain
            if founder.freelancing { delta -= 0.4 }
            if let b = venture.board, b.confidence < 0.4 { delta -= 0.8 }
            if runwayWeeks < 8 { delta -= 0.8 }
            if staff.count > 30 { delta -= 0.4 }
            founder.energy = (founder.energy + delta).clamped(0, 100)
            for lane in lanes {
                founder.experience[lane, default: 0] += founder.attentionShare(for: lane, among: lanes)
            }
        }
    }

    private mutating func updateStaff(_ fx: ComboEffects) {
        var quitters: [Employee] = []
        for i in staff.indices {
            var m = staff[i].morale
            m += (0.7 - m) * 0.05
            if crunch { m -= 0.025 }
            if cash < 0 { m -= 0.03 }
            if staff[i].lane == .websites { m -= fx.moraleDrain }
            staff[i].morale = m.clamped(0, 1)
            if staff[i].morale < 0.3 && rng.chance(0.04) { quitters.append(staff[i]) }
        }
        for q in quitters {
            staff.removeAll { $0.id == q.id }
            addLog("\(q.name) quit. Morale had been low for a while.", .bad, lane: q.lane)
        }
    }

    // MARK: Progress and endings

    private mutating func updateStreaks() {
        let allProfitable = !lanes.isEmpty && lanes.allSatisfy { trailingProfit(lane: $0, weeks: 13) > 0 }
        stats.profitableStreak = allProfitable ? stats.profitableStreak + 1 : 0
        if pivot != nil && lanes.count < 3 && stats.profitableStreak == Self.expansionStreakNeeded {
            addLog("Every lane is profitable. You can expand into another lane.", .milestone)
        }
        if isEcosystem && allProfitable {
            stats.ecosystemWeeks += 1
            if stats.ecosystemWeeks == 52 {
                addLog("A full year as a profitable ecosystem company. The prestige goal is yours, if you can hold it.", .milestone)
            }
        }
    }

    private mutating func checkEndings() {
        guard ending == nil else { return }
        if cash < 0 {
            stats.weeksInRed += 1
            if stats.weeksInRed == 1 {
                addLog("You're overdrawn. Eight weeks in the red and it's over.", .bad)
            }
            if stats.weeksInRed >= 8 { end(.bankrupt); return }
        } else {
            stats.weeksInRed = 0
        }
        if founder.energy <= 0 { end(.burnout); return }
        if week >= Self.lengthWeeks { end(.indie) }
    }
}
