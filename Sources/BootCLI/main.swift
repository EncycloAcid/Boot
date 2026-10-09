import BootCore
import Foundation

// Terminal harness for Boot.
//
//   boot-cli                          interactive game
//   boot-cli auto <lane> [seed]       watch the autopilot play one game
//   boot-cli balance [runs]           autopilot many games per lane, print outcome stats

let args = Array(CommandLine.arguments.dropFirst())

func parseLane(_ s: String) -> Lane? {
    switch s.lowercased() {
    case "games", "game": .games
    case "web", "websites", "website": .websites
    case "apps", "app": .apps
    case "social": .social
    case "hardware", "hw": .hardware
    case "ai": .ai
    default: nil
    }
}

func printEnding(_ e: EndingReport) {
    print("\n==============================================")
    print("  \(e.title.uppercased())  (\(e.kind.name), \(GameClock.label(e.week)))")
    print("==============================================")
    print(e.summary)
    print("")
    for line in e.story { print("  " + line) }
    print("\nFounder take-home: \(Format.money(e.founderTakeHome))")
}

// MARK: Balance

func balance(runs: Int) {
    print("Autopilot balance run: \(runs) games per starting lane\n")
    print("lane       ending distribution                                   median end   median peak val")
    for lane in Lane.allCases {
        var endings: [EndingKind: Int] = [:]
        var weeks: [Int] = []
        var peaks: [Double] = []
        var pivots: [String: Int] = [:]
        for i in 0..<runs {
            var s = GameState(companyName: "Bot \(i)", founderName: "Bot", lane: lane, seed: UInt64(1000 + i))
            Autopilot().playToEnd(&s)
            if let e = s.ending {
                endings[e.kind, default: 0] += 1
                weeks.append(e.week)
            }
            peaks.append(s.stats.peakValuation)
            if let p = s.pivot { pivots["\(p.trigger.rawValue)", default: 0] += 1 }
        }
        weeks.sort()
        peaks.sort()
        let dist = EndingKind.allCases.compactMap { k in endings[k].map { "\(k.rawValue) \($0)" } }.joined(separator: ", ")
        let name = lane.rawValue.padding(toLength: 10, withPad: " ", startingAt: 0)
        print("\(name) \(dist.padding(toLength: 54, withPad: " ", startingAt: 0)) \(GameClock.label(weeks[weeks.count / 2]).padding(toLength: 12, withPad: " ", startingAt: 0)) \(Format.money(peaks[peaks.count / 2]))")
        print("           pivots: \(pivots.map { "\($0.key) \($0.value)" }.sorted().joined(separator: ", "))")
    }
}

// MARK: Auto

func auto(lane: Lane, seed: UInt64) {
    var s = GameState(companyName: "Autopilot Co", founderName: "Bot", lane: lane, seed: seed)
    var printed = 0
    let pilot = Autopilot()
    while !s.isOver && s.week <= GameState.lengthWeeks + 5 {
        pilot.playWeek(&s)
        for entry in s.log where entry.id > printed && entry.tone != .neutral {
            print("[\(GameClock.label(entry.week))] \(entry.text)")
        }
        printed = s.log.last?.id ?? printed
        if s.week % 52 == 0 {
            print("--- \(s.dateLabel): cash \(Format.money(s.cash)), valuation \(Format.money(s.valuation)), staff \(s.staff.count), energy \(Int(s.founder.energy)), lanes \(s.lanes.map(\.name).joined(separator: "+"))")
        }
    }
    if let e = s.ending { printEnding(e) }
}

// MARK: Interactive

struct Session {
    var state: GameState
    var seen = 0

    /// Print news that arrived since the last command.
    mutating func fresh() {
        for e in state.log where e.id > seen {
            let mark = switch e.tone { case .good: "+"; case .bad: "!"; case .milestone: "*"; case .neutral: " " }
            print(" \(mark) [\(GameClock.label(e.week))] \(e.text)")
        }
        seen = state.log.last?.id ?? seen
    }

    func dashboard() {
        let s = state
        print("\n=== \(s.companyName) | \(s.dateLabel) | \(s.world.era.name) ===")
        let runway = s.runwayWeeks.isFinite ? "\(Int(s.runwayWeeks))w runway" : "profitable"
        print("Cash \(Format.money(s.cash)) (\(runway)) | Valuation \(Format.money(s.valuation)) | Reputation \(Int(s.reputation.score)) | Energy \(Int(s.founder.energy))\(s.crunch ? " | CRUNCH" : "")")
        print("HQ: \(s.headquarters.name) | Staff \(s.staff.count) | Payroll \(Format.money(s.payroll))/wk\(s.venture.board.map { " | Board confidence \(Int($0.confidence * 100))%" } ?? "")")
        for lane in s.lanes {
            let rev = s.trailingRevenue(lane: lane, weeks: 4) / 4
            let profit = s.trailingProfit(lane: lane, weeks: 4) / 4
            print("  [\(lane.rawValue)] \(s.statusLine(for: lane))  | \(Format.money(rev))/wk rev, \(Format.money(profit))/wk profit")
        }
        for c in s.crossovers where c.built || c.buildWeeksLeft < c.combo.buildWeeks {
            print("  combo \(c.combo.product): \(c.built ? "live" : "building, \(c.buildWeeksLeft)w"), \(c.combo.dial.lowercased()) \(Int(c.intensity * 100))%, pressure \(Int(c.pressure * 100))%")
        }
        if s.isEcosystem { print("  ECOSYSTEM: \(s.stats.ecosystemWeeks) profitable weeks") }
    }

    func news(_ n: Int = 8) {
        for e in state.log.suffix(n) {
            let mark = switch e.tone { case .good: "+"; case .bad: "!"; case .milestone: "*"; case .neutral: " " }
            print(" \(mark) [\(GameClock.label(e.week))] \(e.text)")
        }
    }

    func help() {
        print("""
        Commands:
          n [weeks]                      advance (default 1 week; stops for decisions)
          status | news | staff | hires | combos | money
          hire <id> [lane]  fire <id>  assign <id> <lane>  focus <lane> <0-1>
          crunch on|off  vacation  raise  ipo  expand <lane> [vc]
          game start small|medium|large   game launch   game cancel
          web accept <id>  web decline <id>  web policy absorb|bill  web offers
          app monetize ads|subscription|paid   app updates small|big
          social moderation <0-1>   social ads <0-1>
          hw design phone|laptop|headphones budget|flagship   hw order <units>   hw launch
          ai train small|medium|frontier   ai focus chatbot|api|enterprise   ai gpus <n>
          combo build <n>   combo dial <n> <0-1>     (n = number from 'combos')
          save <file>  load <file>  quit
        """)
    }

    mutating func decide() {
        if let p = state.pendingPivot {
            print("\n*** THE PIVOT POINT: \(p.headline) ***")
            print(p.body)
            print("Choices: pivot <lane> expand|pivot [vc]\(p.trigger == .buyoutOffer ? "  |  sell (sell the whole company)" : "")")
            print("Lanes: \(state.pivotChoices.map(\.rawValue).joined(separator: ", "))\(p.suggestedLane.map { "  (suggested: \($0.rawValue))" } ?? "")")
            return
        }
        if let e = state.events.first {
            print("\n*** \(e.title) ***")
            print(e.body)
            for (i, c) in e.choices.enumerated() { print("  \(i + 1). \(c.label): \(c.detail)") }
            print("Choose with: choose <number>")
        }
    }

    mutating func run(_ line: String) throws -> Bool {
        let w = line.split(separator: " ").map(String.init)
        guard let cmd = w.first?.lowercased() else { return true }
        func arg(_ i: Int) -> String { w.count > i ? w[i].lowercased() : "" }
        func num(_ i: Int) -> Double? { Double(arg(i)) }
        func int(_ i: Int) -> Int? { Int(arg(i)) }

        switch cmd {
        case "quit", "q", "exit": return false
        case "help", "h", "?": help()
        case "status": dashboard()
        case "news", "log": news(20)
        case "n", "next":
            let n = try state.advance(weeks: int(1) ?? 1)
            if n > 1 { print("Advanced \(n) weeks.") }
            fresh()
            dashboard()
        case "choose":
            guard let n = int(1) else { throw ActionError.invalid("choose <number>") }
            try state.choose(n - 1)
        case "pivot":
            guard let lane = parseLane(arg(1)) else { throw ActionError.invalid("pivot <lane> expand|pivot [vc]") }
            let mode: PivotMode = arg(2) == "pivot" ? .pivot : .expand
            try state.resolvePivot(to: lane, mode: mode, fundWithVenture: arg(3) == "vc")
        case "sell": try state.acceptPivotBuyout()
        case "staff":
            for e in state.staff {
                print("  #\(e.id) \(e.name), \(e.role.name) skill \(e.skill), \(Format.money(e.salary))/wk, morale \(Int(e.morale * 100))%, \(e.lane?.rawValue ?? "unassigned")")
            }
            if state.staff.isEmpty { print("  Just you.") }
        case "hires":
            for c in state.candidates {
                print("  #\(c.id) \(c.name), \(c.role.name) skill \(c.skill), asks \(Format.money(c.askingSalary))/wk\(c.contestedBy.map { " (\($0) is circling)" } ?? "")")
            }
        case "hire":
            guard let id = int(1) else { throw ActionError.invalid("hire <id> [lane]") }
            try state.hire(id, lane: parseLane(arg(2)))
        case "fire":
            guard let id = int(1) else { throw ActionError.invalid("fire <id>") }
            try state.fire(id)
        case "assign":
            guard let id = int(1), let lane = parseLane(arg(2)) else { throw ActionError.invalid("assign <id> <lane>") }
            try state.assign(id, to: lane)
        case "focus":
            guard let lane = parseLane(arg(1)), let v = num(2) else { throw ActionError.invalid("focus <lane> <0-1>") }
            state.setFocus(lane, v)
        case "crunch": try state.setCrunch(arg(1) != "off")
        case "vacation": state.takeVacation()
        case "raise":
            try state.raiseRound()
        case "ipo": try state.goPublic()
        case "expand":
            guard let lane = parseLane(arg(1)) else { throw ActionError.invalid("expand <lane> [vc]") }
            try state.expand(into: lane, fundWithVenture: arg(2) == "vc")
        case "money":
            let l = state.ledger.suffix(4)
            for wk in l {
                print("  \(GameClock.label(wk.week)): revenue \(Format.money(wk.totalRevenue)), costs \(Format.money(wk.totalCosts)), cash \(Format.money(wk.cash))")
            }
            switch state.roundOffer() {
            case .success(let o): print("  Investors would put in \(Format.money(o.amount)) (\(o.name)) for \(Format.percent(o.dilution)).")
            case .failure(let e): print("  Fundraising: \(e.message)")
            }
            print("  IPO: \(state.ipoBlocker ?? "ready")")
            print("  Expansion: \(state.expansionBlocker ?? "ready")")
        case "combos":
            for (i, c) in state.crossovers.enumerated() {
                let status = c.built ? "live" : c.buildWeeksLeft < c.combo.buildWeeks ? "building" : "costs \(Format.money(c.combo.buildCost))"
                print("  \(i + 1). \(c.combo.product) [\(status)] \(c.combo.pitch)")
                print("     Problem: \(c.combo.problem)  Dial: \(c.combo.dial) \(Int(c.intensity * 100))%")
            }
            if state.crossovers.isEmpty { print("  Combos unlock when you run two lanes.") }
        case "combo":
            guard let n = int(2), state.crossovers.indices.contains(n - 1) else { throw ActionError.invalid("combo build|dial <n> [0-1]") }
            let c = state.crossovers[n - 1].combo
            if arg(1) == "build" { try state.buildCrossover(c) } else { try state.setIntensity(c, num(3) ?? 0.5) }
        case "game":
            switch arg(1) {
            case "start": try state.startGame(GameScope(rawValue: arg(2)) ?? .small)
            case "launch": try state.launchGame()
            case "cancel": try state.cancelGame()
            default:
                for r in state.games?.releases.prefix(5) ?? [] {
                    print("  \(r.title): \(r.review) (\(r.verdict)), \(Format.money(r.lifetimeSales)) lifetime")
                }
            }
        case "web":
            switch arg(1) {
            case "accept":
                guard let id = int(2) else { throw ActionError.invalid("web accept <id>") }
                try state.acceptContract(id)
            case "decline":
                guard let id = int(2) else { throw ActionError.invalid("web decline <id>") }
                try state.declineContract(id)
            case "policy": try state.setScopePolicy(arg(2) == "bill" ? .billChanges : .absorb)
            default:
                for o in state.web?.offers ?? [] {
                    print("  #\(o.id) \(o.client): \(Format.money(o.value)), ~\(Int(o.work)) work, due in \(o.weeksAllowed)w")
                }
                for c in state.web?.active ?? [] {
                    print("  active \(c.client): \(Int(c.completion * 100))% done, satisfaction \(Int(c.satisfaction * 100))%\(c.isOverdue ? " OVERDUE" : "")")
                }
            }
        case "app":
            if arg(1) == "monetize" { try state.setMonetization(AppMonetization(rawValue: arg(2)) ?? .ads) }
            if arg(1) == "updates" { try state.setUpdateStyle(UpdateStyle(rawValue: arg(2)) ?? .small) }
        case "social":
            guard let v = num(2) else { throw ActionError.invalid("social moderation|ads <0-1>") }
            if arg(1) == "moderation" { try state.setModeration(v) } else { try state.setAdLoad(v) }
        case "hw":
            switch arg(1) {
            case "design":
                try state.startDevice(DeviceCategory(rawValue: arg(2)) ?? .phone, DeviceTier(rawValue: arg(3)) ?? .budget)
            case "order":
                guard let n = int(2) else { throw ActionError.invalid("hw order <units>") }
                try state.orderUnits(n)
            case "launch": try state.launchDevice()
            default: break
            }
        case "ai":
            switch arg(1) {
            case "train": try state.startTraining(TrainingSize(rawValue: arg(2)) ?? .small)
            case "focus": try state.setAIFocus(AIProduct(rawValue: arg(2)) ?? .chatbot)
            case "gpus": try state.buyGPUs(int(2) ?? 1)
            default: break
            }
        case "save":
            let data = try JSONEncoder().encode(state)
            try data.write(to: URL(fileURLWithPath: w.count > 1 ? w[1] : "boot-save.json"))
            print("Saved.")
        case "load":
            let data = try Data(contentsOf: URL(fileURLWithPath: w.count > 1 ? w[1] : "boot-save.json"))
            state = try JSONDecoder().decode(GameState.self, from: data)
            seen = state.log.last?.id ?? 0
            dashboard()
        default:
            print("Unknown command. Type 'help'.")
        }
        return true
    }
}

func interactive() {
    print("BOOT: from a cramped apartment to... somewhere.\n")
    print("Company name?", terminator: " ")
    let name = readLine().flatMap { $0.isEmpty ? nil : $0 } ?? "Garage Labs"
    print("Pick your lane:")
    for (i, lane) in Lane.allCases.enumerated() {
        print("  \(i + 1). \(lane.name)\(lane.isCapitalIntensive ? " (hard mode: starts with venture money and a board)" : "")")
        print("     \(lane.pitch)")
    }
    print(">", terminator: " ")
    let pick = Int(readLine() ?? "") ?? 1
    let lane = Lane.allCases[(pick - 1).clamped(to: 0...(Lane.allCases.count - 1))]
    var session = Session(state: GameState(companyName: name, founderName: "You", lane: lane))
    session.fresh()
    session.dashboard()
    print("Type 'help' for commands.")
    while true {
        if let e = session.state.ending {
            printEnding(e)
            return
        }
        session.decide()
        print(">", terminator: " ")
        guard let line = readLine() else { return }
        do {
            if try !session.run(line) { return }
            session.fresh()
        } catch let e as ActionError {
            print("  \(e.message)")
        } catch {
            print("  \(error.localizedDescription)")
        }
    }
}

extension Comparable {
    func clamped(to r: ClosedRange<Self>) -> Self { min(max(self, r.lowerBound), r.upperBound) }
}

switch args.first {
case "balance":
    balance(runs: args.count > 1 ? Int(args[1]) ?? 20 : 20)
case "auto":
    auto(lane: parseLane(args.count > 1 ? args[1] : "games") ?? .games,
         seed: args.count > 2 ? UInt64(args[2]) ?? 1 : 1)
default:
    interactive()
}
