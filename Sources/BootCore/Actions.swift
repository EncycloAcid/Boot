import Foundation

/// Everything the player can do between weeks.
extension GameState {
    private func requireLive() throws {
        if ending != nil { throw ActionError.gameOver }
    }

    private func require(_ lane: Lane) throws {
        try requireLive()
        guard lanes.contains(lane) else { throw ActionError.notAvailable("You don't run a \(lane.businessName.lowercased()).") }
    }

    // MARK: Company

    public mutating func setCrunch(_ on: Bool) throws {
        try requireLive()
        crunch = on
        addLog(on ? "Crunch mode. Everyone's staying late." : "Crunch is over. People go home at a normal hour.", on ? .bad : .neutral)
    }

    public mutating func setFreelancing(_ on: Bool) throws {
        try requireLive()
        guard founder.freelancing != on else { return }
        founder.freelancing = on
        addLog(on ? "You're taking freelance gigs to cover rent. The company gets what's left of you."
                  : "No more side gigs. You're back on the company full time.", .neutral)
    }

    public mutating func takeVacation() {
        guard !founder.isOnVacation else { return }
        founder.vacationWeeksLeft = 2
        stats.vacations += 1
        addLog("You're taking two weeks off. Nothing moves without you.", .neutral)
    }

    public mutating func setFocus(_ lane: Lane, _ weight: Double) {
        guard lanes.contains(lane) else { return }
        founder.focus[lane] = weight.clamped(0, 1)
    }

    public mutating func raiseRound() throws {
        try requireLive()
        switch roundOffer() {
        case .success(let offer): takeRound(offer)
        case .failure(let e): throw e
        }
    }

    public mutating func rename(_ name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty { companyName = trimmed }
    }

    // MARK: Staff

    public mutating func hire(_ candidateID: Int, lane: Lane? = nil) throws {
        try requireLive()
        guard let i = candidates.firstIndex(where: { $0.id == candidateID }) else {
            throw ActionError.invalid("That candidate took another job.")
        }
        let c = candidates[i]
        let fee = c.askingSalary * 2
        guard cash >= fee else { throw ActionError.insufficientFunds("Recruiting costs \(money(fee)).") }
        candidates.remove(at: i)
        spend(fee, lane: nil)
        let e = hireDirect(c, lane: lane)
        addLog("Hired \(e.name), \(e.role.name.lowercased()) (skill \(e.skill)), at \(money(e.salary))/week.", .good, lane: e.lane)
    }

    public mutating func fire(_ employeeID: Int) throws {
        try requireLive()
        guard let i = staff.firstIndex(where: { $0.id == employeeID }) else { throw ActionError.invalid("No such employee.") }
        let e = staff.remove(at: i)
        spend(e.salary * 4, lane: e.lane)
        for j in staff.indices { staff[j].morale -= 0.03 }
        addLog("Let \(e.name) go with four weeks' severance.", .neutral, lane: e.lane)
    }

    public mutating func assign(_ employeeID: Int, to lane: Lane?) throws {
        try requireLive()
        guard let i = staff.firstIndex(where: { $0.id == employeeID }) else { throw ActionError.invalid("No such employee.") }
        if let lane, !lanes.contains(lane) { throw ActionError.notAvailable("You don't run \(lane.name).") }
        staff[i].lane = lane
    }

    // MARK: Games

    public mutating func startGame(_ scope: GameScope) throws {
        try require(.games)
        try games!.startProject(scope: scope, rng: &rng)
        addLog("Started development on \(games!.project!.title), a \(scope.name.lowercased()) game.", .neutral, lane: .games)
    }

    public mutating func cancelGame() throws {
        try require(.games)
        guard let p = games?.project else { throw ActionError.invalid("Nothing in development.") }
        games!.cancelProject()
        addLog("Cancelled \(p.title) after \(p.weeksInDev) weeks.", .bad, lane: .games)
    }

    public mutating func launchGame() throws {
        try require(.games)
        let report = try games!.launch(context(for: .games), rng: &rng)
        handle(report.incidents, lane: .games)
    }

    // MARK: Websites

    public mutating func acceptContract(_ id: Int) throws {
        try require(.websites)
        let upfront = try web!.accept(id, week: week)
        cash += upfront
        recordRevenue(upfront, lane: .websites)
    }

    public mutating func declineContract(_ id: Int) throws {
        try require(.websites)
        web!.decline(id)
    }

    public mutating func setScopePolicy(_ policy: ScopePolicy) throws {
        try require(.websites)
        web!.policy = policy
    }

    // MARK: Apps

    public mutating func setMonetization(_ m: AppMonetization) throws {
        try require(.apps)
        guard apps!.monetization != m else { return }
        apps!.monetization = m
        if apps!.isLive {
            apps!.users *= 0.9
            addLog("\(apps!.name) switched to \(m.name.lowercased()). Some users left over the change.", .neutral, lane: .apps)
        }
    }

    public mutating func setUpdateStyle(_ s: UpdateStyle) throws {
        try require(.apps)
        apps!.updateStyle = s
    }

    // MARK: Social

    public mutating func setModeration(_ level: Double) throws {
        try require(.social)
        social!.moderation = level.clamped(0, 1)
    }

    public mutating func setAdLoad(_ level: Double) throws {
        try require(.social)
        social!.adLoad = level.clamped(0, 1)
    }

    // MARK: Hardware

    public mutating func startDevice(_ category: DeviceCategory, _ tier: DeviceTier) throws {
        try require(.hardware)
        try hardware!.startDesign(DeviceSpec(category: category, tier: tier), rng: &rng)
        addLog("Design work starts on the \(hardware!.design!.name).", .neutral, lane: .hardware)
    }

    public mutating func orderUnits(_ units: Int) throws {
        try require(.hardware)
        var lab = hardware!
        let (model, cost) = try lab.order(units: units, week: week)
        guard cash >= cost else {
            throw ActionError.insufficientFunds("\(units) units cost \(money(cost)) up front. You have \(money(cash)).")
        }
        hardware = lab
        spend(cost, lane: .hardware)
        addLog("Booked \(units) units of the \(model) for \(money(cost)). They arrive in \(HardwareLab.leadTimeWeeks) weeks.", .neutral, lane: .hardware)
    }

    public mutating func launchDevice() throws {
        try require(.hardware)
        let report = try hardware!.launch(context(for: .hardware), rng: &rng)
        cash += report.revenue
        recordRevenue(report.revenue, lane: .hardware)
        handle(report.incidents, lane: .hardware)
    }

    // MARK: AI

    public mutating func startTraining(_ size: TrainingSize) throws {
        try require(.ai)
        let weekly = ai!.weeklyComputeCost(units: size.computeUnits, ctx: context(for: .ai))
        let total = weekly * Double(size.weeks)
        guard cash >= weekly * 4 else {
            throw ActionError.insufficientFunds("A \(size.name.lowercased()) burns \(money(weekly))/week. You can't cover a month of it.")
        }
        try ai!.startTraining(size, rng: &rng)
        addLog("Training \(ai!.training!.codename) started. Estimated compute bill: \(money(total)).", .neutral, lane: .ai)
    }

    public mutating func setAIFocus(_ p: AIProduct) throws {
        try require(.ai)
        ai!.focus = p
    }

    public mutating func buyGPUs(_ units: Int) throws {
        try require(.ai)
        let cost = Double(units) * AILab.gpuUnitPrice * world.era.gpuPrice
        guard cash >= cost else { throw ActionError.insufficientFunds("\(units) GPU units cost \(money(cost)).") }
        spend(cost, lane: .ai)
        ai!.gpusOwned += units
        addLog("Bought \(units) GPU cluster units for \(money(cost)).", .neutral, lane: .ai)
    }

    // MARK: Crossovers

    public mutating func buildCrossover(_ combo: Combo) throws {
        try requireLive()
        guard let i = crossovers.firstIndex(where: { $0.combo == combo }) else {
            throw ActionError.notAvailable("You need both \(combo.lanes.0.name) and \(combo.lanes.1.name) for that.")
        }
        guard !crossovers[i].built, crossovers[i].buildWeeksLeft == combo.buildWeeks else {
            throw ActionError.busy("\(combo.product) is already underway.")
        }
        guard cash >= combo.buildCost else {
            throw ActionError.insufficientFunds("\(combo.product) costs \(money(combo.buildCost)) to build.")
        }
        spend(combo.buildCost, lane: nil)
        crossovers[i].buildWeeksLeft -= 1
        addLog("Building \(combo.product). Ready in about \(combo.buildWeeks) weeks.", .neutral)
    }

    public mutating func setIntensity(_ combo: Combo, _ value: Double) throws {
        try requireLive()
        guard let i = crossovers.firstIndex(where: { $0.combo == combo }) else {
            throw ActionError.notAvailable("That combo isn't available.")
        }
        crossovers[i].intensity = value.clamped(0, 1)
    }
}
