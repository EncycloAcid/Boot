import Foundation

public enum ActionError: Error, LocalizedError, Equatable, Sendable {
    case invalid(String)
    case busy(String)
    case insufficientFunds(String)
    case notAvailable(String)
    case decisionPending
    case gameOver

    public var message: String {
        switch self {
        case let .invalid(m), let .busy(m), let .insufficientFunds(m), let .notAvailable(m): m
        case .decisionPending: "A decision is waiting on you first."
        case .gameOver: "The game is over."
        }
    }

    public var errorDescription: String? { message }
}

/// One week of the books.
public struct WeekLedger: Codable, Sendable {
    public var week: Int
    public var revenue: [Lane: Double] = [:]
    public var costs: [Lane: Double] = [:]
    /// Rent, crossover programs and anything not tied to one lane.
    public var overhead: Double = 0
    public var otherRevenue: Double = 0
    public var cash: Double = 0

    public var totalRevenue: Double { revenue.values.reduce(0, +) + otherRevenue }
    public var totalCosts: Double { costs.values.reduce(0, +) + overhead }
    public var profit: Double { totalRevenue - totalCosts }

    public func profit(_ lane: Lane) -> Double { (revenue[lane] ?? 0) - (costs[lane] ?? 0) }
}

public struct Headquarters: Sendable {
    public var name: String
    public var rent: Double

    static func forHeadcount(_ n: Int) -> Headquarters {
        switch n {
        case 0...2: Headquarters(name: "Cramped apartment", rent: 600)
        case 3...8: Headquarters(name: "Coworking desks", rent: 2_500)
        case 9...25: Headquarters(name: "Converted warehouse office", rent: 9_000)
        case 26...80: Headquarters(name: "Downtown floor", rent: 35_000)
        default: Headquarters(name: "Campus", rent: 120_000)
        }
    }
}

public struct GameState: Codable, Sendable {
    /// Fifteen years.
    public static let lengthWeeks = 780
    public static let startingCash = 25_000.0

    public let seed: UInt64
    var rng: SeededRNG
    public internal(set) var week = 0
    public var companyName: String
    public internal(set) var founder: Founder
    public let startingLane: Lane
    public internal(set) var lanes: [Lane] = []

    public internal(set) var games: GamesStudio?
    public internal(set) var web: WebAgency?
    public internal(set) var apps: AppStudio?
    public internal(set) var social: SocialPlatform?
    public internal(set) var hardware: HardwareLab?
    public internal(set) var ai: AILab?

    public internal(set) var staff: [Employee] = []
    public internal(set) var candidates: [Candidate] = []
    public internal(set) var cash: Double = startingCash
    public internal(set) var crunch = false
    public internal(set) var reputation = Reputation()
    public internal(set) var world = World()
    public internal(set) var venture = Venture()
    public internal(set) var crossovers: [Crossover] = []
    public internal(set) var pivot: PivotRecord?
    public internal(set) var pendingPivot: PivotOffer?
    public internal(set) var expansions: [ExpansionRecord] = []
    public internal(set) var events: [PendingEvent] = []
    public internal(set) var log: [LogEntry] = []
    public internal(set) var ledger: [WeekLedger] = []
    public internal(set) var stats = Stats()
    public internal(set) var ending: EndingReport?
    private var idCounter = 1

    /// Start a new company. Hardware and AI need a seed round from day one.
    public init(companyName: String, founderName: String, lane: Lane, seed: UInt64 = UInt64.random(in: 1...UInt64.max)) {
        self.seed = seed
        rng = SeededRNG(seed: seed)
        self.companyName = companyName
        founder = Founder(name: founderName)
        startingLane = lane
        world.rivals = World.makeRivals()
        world.eraLength = rng.int(70...110)
        ledger = [WeekLedger(week: 0, cash: cash)]
        openLane(lane)
        refreshTalentPool()
        addLog("Day one. A laptop, a cramped apartment, and \(money(cash)) in savings.", .milestone)
        if lane.isCapitalIntensive, case .success(let round) = ventureForLane(lane) {
            takeRound(round)
            addLog("\(lane.name) eats money. You raised a \(money(round.amount)) seed round and gained a board.", .neutral)
        }
    }

    // MARK: Queries

    public var isOver: Bool { ending != nil }
    public var needsDecision: Bool { !events.isEmpty || pendingPivot != nil }
    public var dateLabel: String { GameClock.label(week) }
    public var headquarters: Headquarters { .forHeadcount(staff.count) }
    public var payroll: Double { staff.reduce(0) { $0 + $1.salary } }

    public func crossover(_ combo: Combo) -> Crossover? {
        crossovers.first { $0.combo == combo }
    }

    /// Combos you could build with your current lanes, built or not.
    public var availableCombos: [Combo] { Combo.available(for: lanes) }

    public func trailingRevenue(lane: Lane? = nil, weeks: Int, offset: Int = 0) -> Double {
        window(weeks, offset).reduce(0) { sum, w in
            sum + (lane.map { w.revenue[$0] ?? 0 } ?? w.totalRevenue)
        }
    }

    public func trailingProfit(lane: Lane? = nil, weeks: Int) -> Double {
        window(weeks, 0).reduce(0) { sum, w in
            sum + (lane.map { w.profit($0) } ?? w.profit)
        }
    }

    private func window(_ weeks: Int, _ offset: Int) -> ArraySlice<WeekLedger> {
        let end = max(0, ledger.count - offset)
        let start = max(0, end - weeks)
        return ledger[start..<end]
    }

    /// Average weekly spend over the last month.
    public var weeklyBurn: Double {
        let w = window(4, 0)
        guard !w.isEmpty else { return payroll + headquarters.rent }
        return w.reduce(0) { $0 + $1.totalCosts } / Double(w.count)
    }

    /// Weeks until the money runs out at the current net burn. Infinite when profitable.
    public var runwayWeeks: Double {
        // Before the first week closes, assume you're burning payroll and rent.
        let net = ledger.count > 1 ? trailingProfit(weeks: 4) / 4 : -(payroll + headquarters.rent)
        guard net < 0 else { return .infinity }
        return max(0, cash) / -net
    }

    public func laneValue(_ lane: Lane) -> Double {
        // Blend the trailing year with the latest quarter so one launch spike doesn't dominate.
        let annual = 0.75 * trailingRevenue(lane: lane, weeks: 52) + trailingRevenue(lane: lane, weeks: 13)
        let assets: Double = switch lane {
        case .games: games?.assetValue ?? 0
        case .websites: web?.assetValue ?? 0
        case .apps: apps?.assetValue ?? 0
        case .social: social?.assetValue ?? 0
        case .hardware: hardware?.assetValue ?? 0
        case .ai: ai?.assetValue ?? 0
        }
        return max(0, annual * lane.revenueMultiple * world.era.investorAppetite) + assets
    }

    /// What the businesses are worth, excluding cash in the bank.
    public var enterpriseValue: Double {
        lanes.reduce(0) { $0 + laneValue($1) } * (isEcosystem ? 1.3 : 1)
    }

    /// Enterprise value plus cash. What a buyer or the market would pay.
    public var valuation: Double {
        enterpriseValue + max(0, cash)
    }

    public func staff(in lane: Lane) -> [Employee] { staff.filter { $0.lane == lane } }

    public func staffing(for lane: Lane) -> Staffing {
        var s = Staffing()
        var skillSum = 0.0
        var weight = 0.0
        for e in staff where e.lane == lane {
            s.output += e.output(in: lane)
            s.headcount += 1
            skillSum += Double(e.skill)
            weight += 1
            let points = Double(e.skill) * e.moraleFactor
            switch e.role {
            case .marketer: s.marketing += points
            case .operator: s.operations += points
            case .researcher: s.research += points
            default: break
            }
        }
        if !founder.isOnVacation {
            let share = founder.attentionShare(for: lane, among: lanes)
            let energy = 0.4 + 0.6 * founder.energy / 100
            let bonus = founder.skillBonus(in: lane)
            let freelance = founder.freelancing ? 0.35 : 1
            s.output += (4 + bonus) * energy * share * freelance
            s.marketing += 1 * share
            if lane == .ai { s.research += (3 + bonus) * share }
            skillSum += (5 + bonus) * share
            weight += share
        }
        s.averageSkill = weight > 0 ? skillSum / weight : 5
        return s
    }

    func context(for lane: Lane, effects: ComboEffects? = nil) -> LaneContext {
        var mods = effects?.mods[lane] ?? LaneModifiers()
        if isEcosystem {
            mods.demand *= stats.ecosystemOpened ? 1.08 : 1.15
            if lane == .apps { mods.churn *= 0.9 }
        }
        return LaneContext(week: week, staffing: staffing(for: lane), mods: mods,
                           reception: reputation.launchReception(for: lane), era: world.era,
                           competition: world.competition(in: lane), crunch: crunch, lane: lane)
    }

    /// Weekly compute bill for a training run of this size, at today's GPU prices.
    public func trainingWeeklyCost(_ size: TrainingSize) -> Double {
        ai?.weeklyComputeCost(units: size.computeUnits, ctx: context(for: .ai)) ?? 0
    }

    /// One line per lane for dashboards.
    public func statusLine(for lane: Lane) -> String {
        switch lane {
        case .games: games?.statusLine ?? ""
        case .websites: web?.statusLine ?? ""
        case .apps: apps?.statusLine ?? ""
        case .social: social?.statusLine ?? ""
        case .hardware: hardware?.statusLine ?? ""
        case .ai: ai?.statusLine ?? ""
        }
    }

    // MARK: Bookkeeping

    mutating func nextID() -> Int {
        defer { idCounter += 1 }
        return idCounter
    }

    mutating func addLog(_ text: String, _ tone: LogTone = .neutral, lane: Lane? = nil) {
        log.append(LogEntry(id: nextID(), week: week, text: text, tone: tone, lane: lane))
        if log.count > 500 { log.removeFirst(log.count - 500) }
    }

    /// Book a one-off cost against this week.
    mutating func recordCost(_ amount: Double, lane: Lane?) {
        guard !ledger.isEmpty else { return }
        if let lane {
            ledger[ledger.count - 1].costs[lane, default: 0] += amount
        } else {
            ledger[ledger.count - 1].overhead += amount
        }
    }

    mutating func recordRevenue(_ amount: Double, lane: Lane?) {
        guard !ledger.isEmpty else { return }
        if let lane {
            ledger[ledger.count - 1].revenue[lane, default: 0] += amount
        } else {
            ledger[ledger.count - 1].otherRevenue += amount
        }
        stats.totalRevenue += amount
    }

    mutating func spend(_ amount: Double, lane: Lane?) {
        cash -= amount
        recordCost(amount, lane: lane)
    }

    mutating func takeRound(_ offer: RoundOffer) {
        venture.take(offer, week: week)
        cash += offer.amount
        addLog("Closed a \(money(offer.amount)) \(offer.name) at a \(money(offer.preMoney)) pre-money valuation. You now own \(Format.percent(venture.founderEquity)).", .milestone)
    }

    mutating func openLane(_ lane: Lane) {
        guard !lanes.contains(lane) else { return }
        lanes.append(lane)
        founder.focus[lane] = 1
        switch lane {
        case .games: games = GamesStudio()
        case .websites: web = WebAgency()
        case .apps: apps = AppStudio(name: rng.pick(Names.apps), monetization: .ads)
        case .social: social = SocialPlatform(name: rng.pick(Names.socials), spark: rng.range(0.55, 1.35))
        case .hardware: hardware = HardwareLab()
        case .ai: ai = AILab()
        }
        for combo in Combo.available(for: lanes) where crossover(combo) == nil {
            crossovers.append(Crossover(combo: combo, buildWeeksLeft: combo.buildWeeks))
            addLog("New combo unlocked: \(combo.product). \(combo.pitch)", .good)
        }
    }

    mutating func closeLane(_ lane: Lane) {
        lanes.removeAll { $0 == lane }
        founder.focus[lane] = nil
        switch lane {
        case .games: games = nil
        case .websites: web = nil
        case .apps: apps = nil
        case .social: social = nil
        case .hardware: hardware = nil
        case .ai: ai = nil
        }
        crossovers.removeAll { $0.combo.lanes.0 == lane || $0.combo.lanes.1 == lane }
    }

    // MARK: Talent

    mutating func makeCandidate(role: Role? = nil) -> Candidate {
        let role = role ?? pickRole()
        let skill = Int(rng.normal(mean: 5, sd: 1.8).rounded()).clamped(to: 1...10)
        var salary = role.baseSalary * (0.5 + Double(skill) * 0.1) * rng.range(0.9, 1.15)
        if role == .researcher { salary *= world.era == .aiGoldRush ? 1.6 : 1 }
        var contested: String?
        var expires = week + 6
        let rival = rng.pick(world.rivals)
        if rng.chance(0.15 + 0.3 * rival.aggression * (skill >= 7 ? 1 : 0.4)) {
            contested = rival.name
            salary *= 1.2
            expires = week + 2
        }
        return Candidate(id: nextID(), name: "\(rng.pick(Names.first)) \(rng.pick(Names.last))", role: role,
                         skill: skill, askingSalary: salary.rounded(), contestedBy: contested, expiresWeek: expires)
    }

    private mutating func pickRole() -> Role {
        // The talent pool leans toward what your lanes need.
        let lane = rng.pick(lanes.isEmpty ? [startingLane] : lanes)
        let roles = Role.allCases
        let weights = roles.map { lane.weight(for: $0) }
        var roll = rng.unit() * weights.reduce(0, +)
        for (role, w) in zip(roles, weights) {
            roll -= w
            if roll <= 0 { return role }
        }
        return .engineer
    }

    mutating func refreshTalentPool() {
        for c in candidates where c.expiresWeek < week {
            if let rival = c.contestedBy {
                addLog("\(c.name) signed with \(rival).", .neutral)
            }
        }
        candidates.removeAll { $0.expiresWeek < week }
        while candidates.count < 5 {
            candidates.append(makeCandidate())
        }
    }

    @discardableResult
    mutating func hireDirect(_ c: Candidate, lane: Lane? = nil) -> Employee {
        let e = Employee(id: c.id, name: c.name, role: c.role, skill: c.skill, salary: c.askingSalary,
                         morale: 0.75, lane: lane ?? bestLane(for: c.role), hiredWeek: week)
        staff.append(e)
        stats.peakHeadcount = max(stats.peakHeadcount, staff.count)
        return e
    }

    func bestLane(for role: Role) -> Lane? {
        lanes.max { $0.weight(for: role) < $1.weight(for: role) }
    }
}
