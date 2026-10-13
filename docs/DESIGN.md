# Boot: Design

A macOS management game. You start solo in a cramped apartment with $25k in savings and pick one of six lanes. Each lane plays like a different kind of management game. Mid-game, the Pivot Point forces you to take on a second lane. Every pairing of lanes unlocks its own crossover product and its own problems. There's no single win condition: you can IPO, get acquired, stay indie, burn out, go bankrupt or get fired by your own board, and the ending you get reflects how you got there.

This document describes the design as it is implemented in `Sources/BootCore`. Numbers here are first-pass tuning, checked with the autopilot balance run (`swift run boot-cli balance`).

## Core loop

- Time moves in **weeks**. A full game is 15 years (780 weeks).
- Each week you make decisions (hire, assign, start projects, set dials), then advance. Advancing stops whenever a decision needs you: an event, a board demand, or the Pivot Point.
- Money in comes from your lanes. Money out is lane costs, payroll and rent. Rent grows with headcount: cramped apartment, coworking desks, warehouse office, downtown floor, campus.
- Eight straight weeks with negative cash is bankruptcy.

### You, the founder

- **Energy** (0 to 100). Crunch, running several lanes, an unhappy board, a short runway and a big org all drain it. Two weeks of vacation restores it, but the company gets none of your time while you're away. At zero, the game ends in burnout.
- **Attention.** With more than one lane, you split your own output between them with focus sliders.
- **Experience.** Weeks spent in a lane make you better at it (up to +4 output). This is the "skills" you keep when you pivot.
- **Freelancing.** You can take outside gigs to cover rent. Steady money, but the company only gets about a third of you.
- **Crunch** is company-wide: +40% output, worse polish, and morale and energy fall fast.

### People

- Five roles: engineer, designer, researcher, marketer, operations. Each lane weights roles differently (AI wants researchers; games want designers; social platforms need operations for moderation).
- Each person has a skill (1 to 10), a salary and morale. Low morale cuts output and makes people quit.
- **Rivals compete for talent.** Some candidates are contested by a rival studio and disappear in two weeks. Rivals also try to poach your best people; you can counter-offer or let them go, which makes that rival stronger.

## The six lanes

### Games: hit-driven and volatile
- Projects come in three scopes (small, mid-size, blockbuster) with very different work and budgets.
- Content progress and **polish** are tracked separately. You can launch once the content is done, or keep polishing for a better review. The studio UI (and `game` in the CLI) shows an **expected review range**, launch-week sales forecast, and weeks-to-ready ETA so the polish call is readable.
- The **review score** drives a launch-week spike (cubic in the review), which then decays fast.
- **Day-one patch:** within 12 weeks of launch, you can spend cash (4× the project's weekly budget) to bump the score and refresh fading sales. A flop rescued into a hit restores some fan trust.
- **Fans** amplify future launches. **Fan trust** drops with every flop and only hits bring it back: the fanbase never forgets.

### Websites: an agency sim
- Client briefs arrive weekly. Accept or pass. 40% is paid up front, the rest on delivery.
- **Scope creep:** clients change the brief at 11pm. Your policy is to absorb it (happy clients, thin margins) or bill for changes (paid, but clients bristle and some walk).
- Overcommit and every project slips past its deadline. **Standing** (word of mouth) brings bigger clients.
- Low risk, low ceiling: offer size and volume are capped.

### Apps: a retention game
- Ship v1.0, then live on the **churn chart** and the **store rank**. Rank drives organic installs, which drive rank.
- Weekly churn rises the longer you go without an update. You choose small, frequent updates or big, rare ones.
- Monetization (ads, subscription, paid upfront) trades installs against revenue and churn.

### Social media: slow, then exponential
- Growth has a network effect: slow at first, then it goes vertical if it **catches on** (around 250k users). A hidden "spark" (product-market fit) decides whether it does, and feature work nudges it up.
- **Server costs outrun revenue early.** Ad revenue only matures at scale (millions of users).
- Success brings **bots**, **toxicity**, **moderation fires** and **press scandals**. Moderation and ad load are sliders.
- Angels fund growth curves, so a growing platform can raise money before it has revenue.

### Hardware: supply chains and timing
- Design a phone, laptop or headphones at a budget or flagship tier. R&D costs every week.
- **Book factory capacity months ahead** (12-week lead time) and guess how many units to make. Guess low and you sell out while scalpers profit. Guess high and you're stuck with warehouses of last year's model, cleared at 70% off.
- Demand spikes with launch hype and fades over a yearly cycle. Brand awareness grows with every unit you sell, so the first device is the hardest.
- **Component shortages** delay orders. **Recalls** (the battery issue) cost you a quarter of production, or you can attempt a quiet fix and hope it doesn't leak.
- Margins are huge if you nail it.

### AI: compute and burn rate
- Training runs (small, medium, frontier) cost a fortune per week and take in-game months.
- **You don't know how good the model is until it finishes.** Results vary widely and some runs are write-offs.
- Each run size has a capability ceiling, so progress means ever-bigger runs.
- Products: consumer chatbot, developer API or enterprise tools. GPUs can be rented (price swings with the era) or bought.
- Regulators show up the moment you get big. Rival labs poach researchers aggressively.
- The highest ceiling in the game and the fastest way to go broke.

## Capital-intensive lanes and investors

Hardware and AI need big money up front.

- **Starting in either is hard mode:** you open with a seed round and a board on day one.
- Any lane can raise rounds (Seed through Series E) once the business is worth enough. Each round dilutes you by about a quarter.
- **The board** sets quarterly growth targets. It gives you a few quarters of grace while you build. Miss targets and confidence falls. Below 40%, the board starts **overruling you** (layoffs, monetize harder, crunch, a sales blitz). Defy them and confidence falls further. At zero, you're **fired from your own company**.
- You can also unlock Hardware and AI at the Pivot Point with the cash from a safer first business.

## The Pivot Point

Between year 2 and year 4, something forces a choice:

| Trigger | When | Effect on a sale |
| --- | --- | --- |
| Buyout offer | Your business is worth something and a rival bids | Sells for 1.3× value; you can also sell the whole company and walk away |
| Market crash | The current era has gutted your lane | Sells for 0.65× value |
| Burnout | Your energy is below 30 | Pivoting gives you a second wind; expanding costs you more |
| Viral by accident | Random | Suggests a lane; picking it comes with a head start |

You pick a second lane and how to take it on:

- **Expand:** keep the first business and run both, with shared staff and cash and your attention split.
- **Pivot:** sell the first business, take the lump sum and your skills, and go all in. People whose skills carry over to the new lane come with you; the rest stay with the buyer.

## Later expansion and the ecosystem company

- Once every lane you run has been profitable for 12 straight weeks, you can add another lane, up to three.
- **Ecosystem company:** Hardware plus software (Apps or Games) plus services (AI, Social or Websites). It boosts demand across all three and the valuation, but antitrust pressure builds the whole time you hold it.
- Holding a profitable ecosystem for a full year is the prestige goal and changes your ending.

## Combos

Every pairing of two lanes (15 in all) unlocks a crossover product only that combination gets, and a problem unique to it. Crossovers cost money and weeks to build. Each has an **intensity dial**: more upside, faster trouble. Trouble builds as pressure; when it boils over you can dial it back (pay to fix it) or ride it out (take the consequence).

| Combo | Crossover | Problem |
| --- | --- | --- |
| Games + Social | Trending Hub: your games trend on your platform | Gamers flood the site and clash with everyone else |
| Websites + Apps | Client Tools Suite: client tools become your own software | Clients notice you're competing with them |
| Apps + Games | Free-to-Play Mobile: huge mobile money | The monetization ethics dial. Squeeze too hard and reputation tanks across every lane |
| Hardware + Apps | Walled Garden: apps run best on your devices | Too much lock-in brings antitrust lawsuits |
| Hardware + AI | Custom Silicon: no more GPU shortage, smarter devices | The chip R&D bill is brutal |
| AI + Social | The Algorithm: addictive recommendations | Engagement soars, and so do the hearings |
| AI + Games | Living NPCs that talk back | A player gets one to say something that goes viral for the wrong reasons |
| Games + Websites | Co-Dev Studio: premium game marketing and co-dev contracts | Agency deadlines collide with launch crunch |
| Games + Hardware | Handheld Console | Fans resent the exclusives |
| Websites + Social | Creator Storefronts | Creators accuse you of rigging the platform |
| Websites + Hardware | Smart Retail Installs | Every install is a support ticket |
| Websites + AI | Autopilot Agency: the model builds the sites | You're automating your own staff, and they know it |
| Apps + Social | Super App | One leak away from a privacy scandal |
| Apps + AI | Assistant App: people stop leaving | Inference costs scale with every user |
| Social + Hardware | Smart Glasses | People don't love being recorded by strangers' glasses |

## The world

- **Eras** last 1.5 to 2.5 years and shift demand per lane, GPU prices and investor appetite: The Bootstrap Years, The AI Gold Rush, The VR Hype Cycle, Crypto Winter, The Remote Work Boom, The Privacy Panic, The Creator Economy, The Retro Revival, The Metaverse Hangover.
- **Rival studios**, one per lane (Gigaplay Interactive, Pixelsmith Collective, Appetite Inc, Fritter, Pear Computing, Hyperion Labs), squeeze your demand, launch competing products and poach your people.
- **Reputation carries across everything.** Scars (predatory monetization, flops, privacy breaches, scandals, recalls, antitrust, layoffs) fade at different speeds and make each lane's audience suspicious in different ways. Ship a predatory mobile game and your social app launches to suspicion.

## Endings

| Ending | How | Variants |
| --- | --- | --- |
| IPO | From year 5, a business worth $500M (excluding cash) with $50M annual revenue, and the board's sign-off | The Ecosystem IPO, IPO Whale-Funded, The Compute IPO, Bootstrapped to the Bell |
| Acquired | Accept a buyout | The Quick Exit (before the Pivot Point), Absorbed by a rival |
| Stayed independent | Reach year 15 | Still Indie, The Lifestyle Business, The Independent Empire, Private and Thriving, Zombie Unicorn |
| Burnout | Energy hits zero | The Company Outlived You |
| Bankrupt | Eight weeks in the red | Burned the Compute Budget |
| Ousted | Board confidence hits zero | Fired From Your Own Company |

The ending screen tells the story of your run: where you started, what forced the Pivot Point and what you chose, the lanes you added, the crossovers you built, your record of hits, flops, recalls and scandals, the scars people still remember, how much you raised and kept, and the eras you lived through.

## Not built yet

- Multiple concurrent products per lane (a second app, two games in parallel).
- Rival companies as full simulations, plus acquisitions you make yourself.
- Art, sound and the apartment-to-campus office view.
- Steam-style achievements per ending variant.
- Proper tuning passes with real playtesters; the current numbers come from autopilot runs.
