@testable import BootCore
import Foundation
import XCTest

final class SetupTests: XCTestCase {
    func testCheapLanesStartSoloAndUnfunded() {
        for lane in [Lane.games, .websites, .apps, .social] {
            let s = GameState(companyName: "T", founderName: "F", lane: lane, seed: 1)
            XCTAssertEqual(s.lanes, [lane])
            XCTAssertEqual(s.cash, GameState.startingCash)
            XCTAssertTrue(s.staff.isEmpty)
            XCTAssertNil(s.venture.board, "\(lane) should not start with a board")
            XCTAssertEqual(s.headquarters.name, "Cramped apartment")
        }
    }

    func testCapitalIntensiveLanesStartWithVentureAndBoard() {
        for lane in [Lane.hardware, .ai] {
            let s = GameState(companyName: "T", founderName: "F", lane: lane, seed: 1)
            XCTAssertNotNil(s.venture.board, "\(lane) is hard mode: it starts with a board")
            XCTAssertGreaterThan(s.cash, 1_000_000)
            XCTAssertLessThan(s.venture.founderEquity, 1)
        }
    }

    func testEveryPairOfLanesHasExactlyOneCombo() {
        XCTAssertEqual(Combo.allCases.count, 15)
        for a in Lane.allCases {
            for b in Lane.allCases where a < b {
                let matches = Combo.allCases.filter {
                    Set([$0.lanes.0, $0.lanes.1]) == Set([a, b])
                }
                XCTAssertEqual(matches.count, 1, "\(a)+\(b)")
                XCTAssertEqual(Combo.between(a, b), Combo.between(b, a))
            }
        }
    }
}

final class SimulationTests: XCTestCase {
    func testSameSeedSameGame() {
        var a = GameState(companyName: "T", founderName: "F", lane: .games, seed: 42)
        var b = GameState(companyName: "T", founderName: "F", lane: .games, seed: 42)
        Autopilot().playToEnd(&a)
        Autopilot().playToEnd(&b)
        XCTAssertEqual(a.week, b.week)
        XCTAssertEqual(a.cash, b.cash)
        XCTAssertEqual(a.ending?.kind, b.ending?.kind)
    }

    func testAutopilotFinishesEveryLaneWithSaneNumbers() {
        for lane in Lane.allCases {
            for seed in UInt64(1)...4 {
                var s = GameState(companyName: "T", founderName: "F", lane: lane, seed: seed)
                Autopilot().playToEnd(&s)
                XCTAssertNotNil(s.ending, "\(lane)/\(seed) never ended")
                XCTAssertLessThanOrEqual(s.week, GameState.lengthWeeks)
                XCTAssertTrue(s.cash.isFinite)
                XCTAssertTrue(s.valuation.isFinite)
                XCTAssertTrue(s.reputation.score >= 0 && s.reputation.score <= 100)
                XCTAssertFalse(s.ending?.story.isEmpty ?? true)
            }
        }
    }

    func testAdvanceBlocksOnDecisions() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .websites, seed: 3)
        s.enqueue(.burnoutWarning)
        XCTAssertThrowsError(try s.advance()) { XCTAssertEqual($0 as? ActionError, .decisionPending) }
        try s.choose(1)
        XCTAssertNoThrow(try s.advance())
    }

    func testSaveAndLoadMidGameContinuesIdentically() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .apps, seed: 9)
        let pilot = Autopilot()
        for _ in 0..<120 { pilot.playWeek(&s) }
        let data = try JSONEncoder().encode(s)
        var restored = try JSONDecoder().decode(GameState.self, from: data)
        for _ in 0..<60 {
            pilot.playWeek(&s)
            pilot.playWeek(&restored)
        }
        XCTAssertEqual(s.week, restored.week)
        XCTAssertEqual(s.cash, restored.cash, accuracy: 0.001)
        XCTAssertEqual(s.lanes, restored.lanes)
    }

    func testBankruptAfterEightWeeksInTheRed() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .websites, seed: 5)
        s.cash = -1_000_000
        for _ in 0..<8 where !s.isOver {
            try s.advance()
        }
        XCTAssertEqual(s.ending?.kind, .bankrupt)
    }

    func testCrunchingOnEmptyBurnsOut() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .games, seed: 5)
        s.founder.energy = 2
        try s.setCrunch(true)
        try s.advance()
        XCTAssertEqual(s.ending?.kind, .burnout)
    }
}

final class LaneTests: XCTestCase {
    func testGameReadinessAndLaunch() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .games, seed: 11)
        try s.startGame(.small)
        XCTAssertThrowsError(try s.launchGame())
        while !(s.games?.project?.isReady ?? false) { try s.advance(); if s.needsDecision { try s.choose(0) } }
        try s.launchGame()
        XCTAssertNil(s.games?.project)
        XCTAssertEqual(s.games?.releases.count, 1)
        XCTAssertEqual(s.stats.releases, 1)
    }

    func testGameDevelopmentFocusChangesAndPersists() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .games, seed: 12)
        try s.startGame(.small)
        try s.setGameFocus(.visuals)
        XCTAssertEqual(s.games?.project?.activeFocus, .visuals)
        XCTAssertGreaterThan(GameFocus.visuals.polishMultiplier, GameFocus.balanced.polishMultiplier)
        XCTAssertLessThan(GameFocus.visuals.workMultiplier, GameFocus.balanced.workMultiplier)

        try s.setGameFocus(.content)
        XCTAssertEqual(s.games?.project?.activeFocus, .content)
        XCTAssertGreaterThan(GameFocus.content.workMultiplier, GameFocus.balanced.workMultiplier)
        XCTAssertLessThan(GameFocus.content.polishMultiplier, GameFocus.balanced.polishMultiplier)

        let data = try JSONEncoder().encode(s)
        let restored = try JSONDecoder().decode(GameState.self, from: data)
        XCTAssertEqual(restored.games?.project?.activeFocus, .content)
    }

    func testOldGameProjectSaveDefaultsToBalancedFocus() throws {
        let oldSave = """
        {"title":"Old Save","scope":"small","progress":12,"polish":3,"weeksInDev":2}
        """.data(using: .utf8)!
        let project = try JSONDecoder().decode(GameProject.self, from: oldSave)
        XCTAssertEqual(project.activeFocus, .balanced)
    }

    func testAcceptingAContractPaysUpfront() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .websites, seed: 2)
        while s.web?.offers.isEmpty ?? true { try s.advance() }
        let offer = s.web!.offers[0]
        let before = s.cash
        try s.acceptContract(offer.id)
        XCTAssertEqual(s.cash, before + offer.value * 0.4, accuracy: 0.01)
        XCTAssertEqual(s.web?.active.count, 1)
    }

    func testHardwareStockArrivesAfterLeadTime() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .hardware, seed: 4)
        try s.startDevice(.headphones, .budget)
        try s.orderUnits(5_000)
        XCTAssertEqual(s.hardware?.orders.count, 1)
        for _ in 0..<HardwareLab.leadTimeWeeks {
            try s.advance()
            while s.needsDecision { try s.choose(1) }
        }
        let stocked = (s.hardware?.design?.preorderedStock ?? 0) + (s.hardware?.inventory ?? 0)
        XCTAssertEqual(stocked, 5_000)
    }

    func testTrainingRunEventuallyFinishes() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .ai, seed: 8)
        try s.startTraining(.small)
        for _ in 0..<TrainingSize.small.weeks {
            try s.advance()
            while s.needsDecision { try s.choose(0) }
        }
        XCTAssertNil(s.ai?.training)
        XCTAssertTrue((s.ai?.capability ?? 0) > 0 || (s.ai?.failedRuns ?? 0) == 1)
    }
}

final class PivotTests: XCTestCase {
    /// Advance until the Pivot Point arrives, settling any other decisions along the way.
    private func reachPivot(_ lane: Lane, seed: UInt64) throws -> GameState {
        var s = GameState(companyName: "T", founderName: "F", lane: lane, seed: seed)
        s.cash = 5_000_000
        while s.pendingPivot == nil && !s.isOver {
            if !s.events.isEmpty { try s.choose(s.events[0].choices.count - 1); continue }
            try s.advance()
        }
        XCTAssertNotNil(s.pendingPivot)
        XCTAssertTrue(GameState.pivotWindow.contains(s.week))
        return s
    }

    func testPivotSellsTheFirstBusiness() throws {
        var s = try reachPivot(.websites, seed: 21)
        let sale = s.pendingPivot!.salePrice
        let cash = s.cash
        try s.resolvePivot(to: .games, mode: .pivot)
        XCTAssertEqual(s.lanes, [.games])
        XCTAssertNil(s.web)
        XCTAssertEqual(s.cash, cash + sale, accuracy: 0.01)
        XCTAssertEqual(s.pivot?.mode, .pivot)
    }

    func testExpandRunsBothLanesAndUnlocksTheirCombo() throws {
        var s = try reachPivot(.games, seed: 22)
        try s.resolvePivot(to: .social, mode: .expand)
        XCTAssertEqual(Set(s.lanes), [.games, .social])
        XCTAssertNotNil(s.crossover(.gamesSocial))
        XCTAssertNil(s.pendingPivot)
    }

    func testCapitalIntensiveLaneNeedsMoneyOrVenture() throws {
        var s = try reachPivot(.websites, seed: 23)
        s.cash = 10_000
        XCTAssertNotNil(s.pivotBlocker(lane: .ai, mode: .expand, fundWithVenture: false))
    }

    func testExpansionWaitsForThePivotAndProfit() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .games, seed: 24)
        XCTAssertNotNil(s.expansionBlocker)
        XCTAssertThrowsError(try s.expand(into: .apps))
        s.pivot = PivotRecord(trigger: .viralAccident, week: 0, from: .games, to: .apps, mode: .expand)
        s.stats.profitableStreak = GameState.expansionStreakNeeded
        XCTAssertNil(s.expansionBlocker)
    }

    func testEcosystemNeedsHardwareSoftwareAndServices() {
        var s = GameState(companyName: "T", founderName: "F", lane: .hardware, seed: 25)
        XCTAssertFalse(s.isEcosystem)
        s.openLane(.apps)
        XCTAssertFalse(s.isEcosystem)
        s.openLane(.ai)
        XCTAssertTrue(s.isEcosystem)
        XCTAssertEqual(s.crossovers.count, 3)
    }
}

final class ReputationTests: XCTestCase {
    func testPredatoryScarFollowsYouIntoSocial() {
        var r = Reputation()
        let before = r.launchReception(for: .social)
        r.scar(.predatoryMonetization, severity: 0.6, week: 0, note: "")
        XCTAssertLessThan(r.launchReception(for: .social), before * 0.8)
        XCTAssertLessThan(r.score, 50)
    }

    func testScarsHeal() {
        var r = Reputation()
        r.scar(.scandal, severity: 0.3, week: 0, note: "")
        for _ in 0..<200 { r.weeklyDrift() }
        XCTAssertTrue(r.scars.isEmpty)
    }

    func testRidingOutMonetizationBacklashLeavesAPredatoryScar() throws {
        var s = GameState(companyName: "T", founderName: "F", lane: .apps, seed: 30)
        s.openLane(.games)
        s.enqueue(.comboProblem(.appsGames))
        try s.choose(1)
        XCTAssertTrue(s.reputation.has(.predatoryMonetization))
    }
}
