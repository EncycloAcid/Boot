package com.boot.core

// Everything the player can do between weeks.

private fun GameState.requireLive() {
    if (ending != null) throw gameOver()
}

private fun GameState.require(lane: Lane) {
    requireLive()
    if (lane !in lanes) throw notAvailable("You don't run a ${lane.businessName.lowercase()}.")
}

// Company

fun GameState.setCrunch(on: Boolean) {
    requireLive()
    if (on && staff.isEmpty()) throw notAvailable("Crunch needs a team. It's just you.")
    crunch = on
    addLog(
        if (on) "Crunch mode. Everyone's staying late." else "Crunch is over. People go home at a normal hour.",
        if (on) LogTone.BAD else LogTone.NEUTRAL,
    )
}

fun GameState.setFreelancing(on: Boolean) {
    requireLive()
    if (founder.freelancing == on) return
    founder.freelancing = on
    addLog(
        if (on) {
            "You're taking freelance gigs to cover rent. The company gets what's left of you."
        } else {
            "No more side gigs. You're back on the company full time."
        },
    )
}

fun GameState.setFocus(lane: Lane, weight: Double) {
    if (lane !in lanes) return
    founder.focus[lane] = weight.clamped(0.0, 1.0)
}

fun GameState.raiseRound() {
    requireLive()
    takeRound(roundOffer().getOrElse { throw it })
}

fun GameState.rename(name: String) {
    val trimmed = name.trim()
    if (trimmed.isNotEmpty()) companyName = trimmed
}

// Staff

fun GameState.hire(candidateId: Int, lane: Lane? = null) {
    requireLive()
    val c = candidates.firstOrNull { it.id == candidateId } ?: throw invalid("That candidate took another job.")
    if (freeDesks <= 0) throw notAvailable("No free desks in the ${office.title.lowercase()}. Move somewhere bigger first.")
    val fee = c.askingSalary * 2
    if (cash < fee) throw insufficientFunds("Recruiting costs ${money(fee)}.")
    candidates = candidates - c
    spend(fee, null)
    val e = hireDirect(c, lane)
    addLog("Hired ${e.name}, ${e.role.title.lowercase()} (skill ${e.skill}), at ${money(e.salary)}/week.", LogTone.GOOD, e.lane)
}

fun GameState.fire(employeeId: Int) {
    requireLive()
    val e = staff.firstOrNull { it.id == employeeId } ?: throw invalid("No such employee.")
    staff = staff - e
    spend(e.salary * 4, e.lane)
    for (other in staff) other.morale -= 0.03
    addLog("Let ${e.name} go with four weeks' severance.", LogTone.NEUTRAL, e.lane)
}

fun GameState.assign(employeeId: Int, lane: Lane?) {
    requireLive()
    val e = staff.firstOrNull { it.id == employeeId } ?: throw invalid("No such employee.")
    if (lane != null && lane !in lanes) throw notAvailable("You don't run ${lane.title}.")
    e.lane = lane
}

// Websites

fun GameState.acceptContract(id: Int) {
    require(Lane.WEBSITES)
    val upfront = web!!.accept(id)
    cash += upfront
    recordRevenue(upfront, Lane.WEBSITES)
}

fun GameState.declineContract(id: Int) {
    require(Lane.WEBSITES)
    web!!.decline(id)
}

fun GameState.setScopePolicy(policy: ScopePolicy) {
    require(Lane.WEBSITES)
    web!!.policy = policy
}

// Apps

fun GameState.setMonetization(m: AppMonetization) {
    require(Lane.APPS)
    val a = apps!!
    if (a.monetization == m) return
    a.monetization = m
    if (a.isLive) {
        a.users *= 0.9
        addLog("${a.name} switched to ${m.title.lowercase()}. Some users left over the change.", LogTone.NEUTRAL, Lane.APPS)
    }
}

fun GameState.setUpdateStyle(s: UpdateStyle) {
    require(Lane.APPS)
    apps!!.updateStyle = s
}

// Social

fun GameState.setModeration(level: Double) {
    require(Lane.SOCIAL)
    social!!.moderation = level.clamped(0.0, 1.0)
}

fun GameState.setAdLoad(level: Double) {
    require(Lane.SOCIAL)
    social!!.adLoad = level.clamped(0.0, 1.0)
}

// Hardware

fun GameState.startDevice(category: DeviceCategory, tier: DeviceTier) {
    require(Lane.HARDWARE)
    val h = hardware!!
    h.startDesign(DeviceSpec(category, tier), rng)
    addLog("Design work starts on the ${h.design!!.name}.", LogTone.NEUTRAL, Lane.HARDWARE)
}

fun GameState.orderUnits(units: Int) {
    require(Lane.HARDWARE)
    val h = hardware!!
    val (model, cost) = h.quote(units)
    if (cash < cost) throw insufficientFunds("$units units cost ${money(cost)} up front. You have ${money(cash)}.")
    h.book(model, units, week, factoryLeadTime)
    spend(cost, Lane.HARDWARE)
    addLog(
        "Booked $units units of the $model for ${money(cost)}. They arrive in $factoryLeadTime weeks.",
        LogTone.NEUTRAL, Lane.HARDWARE,
    )
}

fun GameState.launchDevice() {
    require(Lane.HARDWARE)
    val report = hardware!!.launch(context(Lane.HARDWARE))
    cash += report.revenue
    recordRevenue(report.revenue, Lane.HARDWARE)
    handle(report.incidents, Lane.HARDWARE)
}

// AI

fun GameState.startTraining(size: TrainingSize) {
    require(Lane.AI)
    val lab = ai!!
    if (lab.training != null) throw busy("A training run is already going.")
    val weekly = lab.weeklyComputeCost(size.computeUnits, context(Lane.AI))
    if (cash < weekly * 4) {
        throw insufficientFunds("A ${size.title.lowercase()} burns ${money(weekly)}/week. You can't cover a month of it.")
    }
    lab.startTraining(size, rng)
    addLog("Training ${lab.training!!.codename} started. Estimated compute bill: ${money(weekly * size.weeks)}.", LogTone.NEUTRAL, Lane.AI)
}

fun GameState.setAIFocus(p: AIProduct) {
    require(Lane.AI)
    ai!!.focus = p
}

fun GameState.gpuCost(units: Int): Double = units * AILab.GPU_UNIT_PRICE * world.era.gpuPrice

fun GameState.buyGPUs(units: Int) {
    require(Lane.AI)
    val cost = gpuCost(units)
    if (cash < cost) throw insufficientFunds("$units GPU units cost ${money(cost)}.")
    spend(cost, Lane.AI)
    ai!!.gpusOwned += units
    addLog("Bought $units GPU cluster units for ${money(cost)}.", LogTone.NEUTRAL, Lane.AI)
}

// Crossovers

fun GameState.buildCrossover(combo: Combo) {
    requireLive()
    val c = crossover(combo)
        ?: throw notAvailable("You need both ${combo.first.title} and ${combo.second.title} for that.")
    if (!c.isIdle) throw busy("${combo.product} is already underway.")
    if (cash < combo.buildCost) throw insufficientFunds("${combo.product} costs ${money(combo.buildCost)} to build.")
    spend(combo.buildCost, null)
    c.buildWeeksLeft -= 1
    addLog("Building ${combo.product}. Ready in about ${combo.buildWeeks} weeks.")
}

fun GameState.setIntensity(combo: Combo, value: Double) {
    requireLive()
    val c = crossover(combo) ?: throw notAvailable("That combo isn't available.")
    c.intensity = value.clamped(0.0, 1.0)
}
