# Boot: Design

A company management game for Android and macOS. You start solo in a cramped apartment with $25k in savings and pick one of six lanes. Each lane plays like a different kind of management game. Mid-game, the Pivot Point offers you a second lane (on Android you can turn it down and stay focused). Every pairing of lanes unlocks its own crossover product and its own problems. There's no single win condition: you can IPO, get acquired, stay indie, go bankrupt or get fired by your own board, and the ending you get reflects how you got there.

This document describes the design as it is implemented in `Sources/BootCore` (Swift) and its Kotlin port in `android/core`, which share the same rules and numbers, except for the studio update (the Games studio, research, offices and training), the removal of founder energy and the 0.5 gameplay update (difficulty, goals, the games market, patches and expansions, staff levels, loans and the newer events), which so far exist only in the Kotlin version. Numbers here are first-pass tuning, checked with the autopilot balance run (`swift run boot-cli balance`).

## Core loop

- Time moves in **weeks**. A full game is 15 years (780 weeks).
- On Android the clock runs by itself once you press play, at three speeds, and stops whenever something needs you: an event, a board demand, the Pivot Point, reviews coming in, a game ready to ship, or the account going red. You can also step one week at a time.
- Money in comes from your lanes. Money out is lane costs, payroll and rent.
- **Offices** have desks: cramped apartment (2), coworking desks (6), studio floor (16), downtown office (45), campus. Every hire needs a desk; moving up costs money and raises rent. Squeeze in more people than desks (after a merger, say) and morale drops every week until you move.
- Eight straight weeks with negative cash is bankruptcy (twelve on Easy, six on Hard).
- **Difficulty** is picked at the start. Easy: $60k savings, 20% more demand, cheaper running costs, softer rivals, cheap loans. Hard: $12k, 15% less demand, 12% higher costs, tougher rivals, dear loans.
- **Goals** run from "Ship your first game" to "Be worth $1B": 43 in all, roughly in the order a run meets them. Lane goals only count while you run that lane. The next three unfinished ones are always on the office screen, and each pays out cash, research points, fans or reputation.
- **Going public** doesn't have to end the run. Ring the bell and retire (the classic IPO ending), or list the company and keep running it: the public buys a fifth of the company, the share price follows the business, and each quarter's revenue growth against the market's 3% expectation moves it. Public companies can issue new shares (raise a tenth of market cap, once every six months) or buy shares back, and the CEO can step down at any time. Runs that reach year 15, or retired IPOs, can also keep playing.
- **Industry rankings** place you in a league table of real tech and games companies by value, from Ubisoft up to the trillion-dollar giants. Their values drift with the eras.
- **Quarterly results** land in the news every 13 weeks (earnings, for public companies), and the Company overview shows the last four quarters.
- **Office perks** are bought once and paid for every week. Each lifts where morale settles, and some do more: the arcade speeds up game developers' learning, free lunch brings better candidates, nap pods soften crunch, the research library adds 10% research points, the gym speeds up levelling, the rooftop bar attracts star candidates, and daycare halves quitting. Each office fits more perks than the last (1, 3, 5, 7, 9), and they're drawn in the office.
- **Bank loans** lend against a year's revenue and what the business is worth, repaid weekly over a year at 6% (Easy), 10% or 16% (Hard) interest. One at a time; paying early saves some interest; debt counts against your valuation.

### You, the founder

- **Attention.** With more than one lane, you split your own output between them with focus sliders.
- **Experience.** Weeks spent in a lane make you better at it (up to +4 output). This is the "skills" you keep when you pivot.
- **Freelancing.** You can take outside gigs to cover rent. Steady money, but the company only gets about a third of you.
- **Crunch** stretches the team, not you: +40% of your staff's output, more bugs, worse polish, and morale falls fast. It needs at least one hire.

### People

- Five roles: engineer, designer, researcher, marketer, operations. Each lane weights roles differently (AI wants researchers; games want designers; social platforms need operations for moderation).
- Each person has a skill (1 to 10), a salary and morale. Low morale cuts output and makes people quit.
- Besides skill, everyone has **design, tech and research** talents, weighted by role. Design and tech drive game development; research generates research points.
- **Experience.** People assigned to a lane earn experience every week (more in crunch and while the studio is shipping something). Each level raises their main talent and their pay; every second level raises their skill too. Levels go up to 10.
- **Training** (once researched) sends someone on a course for research points and cash: skill and talents go up by one, and so does their pay.
- **Small events** keep people interesting: someone asks for a raise after a year, a headhunter offers a star hire for a fee, a game jam can turn up a standout candidate, and the R&D tax credit can be banked or spent on a party.
- **Rivals compete for talent.** Some candidates are contested by a rival studio and disappear in two weeks. Rivals also try to poach your best people; you can counter-offer or let them go, which makes that rival stronger.

## The six lanes

### Games: a Game Dev Tycoon style studio
- **Design a game.** Pick a topic (37, ten to start, more from research), one genre or two (action, adventure, RPG, simulation, strategy, casual), an audience (young, everyone, mature, once researched), one or more platforms and a size (small, then medium, large and AAA through research). Topic and genre, genre and platform, and topic and audience each fit great, okay or badly.
- **Three stages, nine focus areas.** Engine, gameplay and story/quests; then dialogues, level design and AI; then world design, graphics and sound. You split each stage's effort with sliders, and each genre rewards a different split.
- **Points.** Every week the team produces **design** and **tech** points, weighted by their talents, the genre's preferred design/tech ratio and the engine. Work also creates **bugs**. Once the content is done the team fixes bugs and adds **polish**. Crunch speeds things up but makes more bugs.
- **Hype** builds during development, at the yearly convention and from marketing campaigns (once researched): a press release, an ad campaign or a TV blitz.
- **Reviews.** Six outlets review every game: IGN (generous, loves big hyped releases), GameSpot (straight, hard on bugs), Game Informer (big on action, RPGs and adventures, scores in quarter points), Eurogamer (champions small creative games, stars), PC Gamer (only PC games; strategy, simulation and tech, percentages) and Edge (hard to please). Each writes a quote built from what stood out. Scores compare the game against your own best so far, so you have to keep getting better to keep scoring well, the way Game Dev Tycoon does.
- **Metascore, players and the breakdown.** The critics' average shows as a Metascore. Players score separately: they care most about gameplay and bugs, remember being overhyped, and reward fan trust; their score decides how slowly sales fade after launch week. Each game gets a breakdown (gameplay, story and world, visuals, sound, polish) from where the effort went against what the genre needs, the engine and the bugs, plus the good and the bad.
- **Price.** Every game is budget (60% of the usual price), standard or premium (140%). Buyers are price sensitive, less so for great games: a 9 earns more at a premium; a 5 earns more cheap. Players reward a bargain and critics call weak premium games overpriced.
- **Sales** follow the score, hype, fans and how many players own the platforms you picked, with a weekly sales chart that decays after launch. Platforms come and go by year, each with a licence fee, a player base and a market share curve; late in the game you can build your own handheld.
- **Game reports** after each launch reveal what worked (fits, focus splits, the design/tech ratio) and store it in a notebook the game designer reads from, so you learn the rules by playing.
- **Engines.** Engine research items (2D/3D graphics, sound, AI, physics, multiplayer, open world and more) can be built into a custom engine for RP and cash; better engines make every game's points go further.
- **Contracts and publishers.** Contract jobs for other companies pay cash and a few research points if you deliver on time. Publisher deals pay half an advance up front for a game to spec, with the rest only if it scores well enough.
- **Sequels and spin-offs** (once researched) keep the topic and start with hype from the series. Keep the genre for a numbered sequel, or change it for a spin-off. A sequel within a year of the original is marked down as rushed, and repeating your last topic and genre without calling it a sequel is marked down too.
- **Franchises.** A first game plus its sequels and spin-offs forms a series, listed under Your franchises. A series that averages 7 or better sells 10% more with each entry (up to 40%), a weak one 5% less (down to 20%), an entry that drops more than half a point from the last wears fans out, and spin-offs get half the effect either way. Sequels are named the way real series name them (numbers, roman numerals, subtitles), and spin-offs by genre (Tactics, Legends, Party).
- **Names.** Suggested titles come from the topic's vocabulary and the genre's habits: RPGs get chronicles and legends, strategy games commands and wars, sims tycoons and simulators. Roll again for another.
- **Trends** come and go (a genre or topic everyone wants), and **Summer Game Fest** each June asks whether to book a small booth, a big booth or a keynote, or skip it.
- **The market.** Fifteen real studios (Electronic Arts, Ubisoft, Rockstar Games, Nintendo, PlayStation Studios, Xbox Game Studios, Valve, Capcom, FromSoftware, CD Projekt Red and others, each with its own typical quality) release games of their own, bigger as the years go by. Every game on sale, theirs and yours, competes on a weekly top-10 sales chart. Launching within six weeks of a rival's 7.5+ game in the same genre costs you about 15% of your launch sales, and the game report says so.
- **Real franchises.** 47 real series release on their own schedules: yearly series (EA Sports FC, Madden, Call of Duty, Just Dance, Fortnite chapters) in the same week each year, the rest every few years with some drift (Grand Theft Auto every decade or so, Half-Life whenever it feels like it). Big games are announced six months ahead, smaller ones three, in the Coming soon list. A 9+ AAA release is a giant: launching within three weeks of one costs you 20% of your launch sales, and when one lands, anything released in the last six weeks loses a fifth of its sales.
- **Game of the Year** is decided every December from the year's four best-reviewed games. If you're nominated you can fly the team out (a small boost to your odds and to morale) or watch the stream. Winning brings fans, fan trust, reputation, a sales bump and hype for whatever you're making next.
- **After launch.** A patch fixes the bugs a game shipped with, for a fee: fans notice and its sales hold up longer. Expansion packs (once researched) are small projects for games that scored 6 or more; owners of the original buy them and its sales pick up. Two per game at most.
- **Fan moments.** Streamers pick up your games, early builds leak, and fans petition for sequels to the ones they loved; a promised sequel starts with extra hype.
- **Side income.** License any engine but the starter kit to other studios for weekly royalties (rival games get slightly better on your tech), and once you've researched merchandise, games that scored 8 or more sell T-shirts and figures for a couple of years.
- **Fans** amplify future launches. **Fan trust** drops with every flop and only hits bring it back: the fanbase never forgets.

### Websites: an agency sim
- **Rates.** Budget brings more, smaller briefs; premium fewer, bigger ones.
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

## Research

Your team generates **research points (RP)** every week: researchers and research talent count most, game development adds some, and an R&D lab adds half again. RP buy items from one tree:

- **Studio:** target audience, marketing campaigns, sequels, custom engines, medium/large/AAA games, multi-genre and multi-platform releases, and new topics.
- **Engine:** features that become parts of custom engines (graphics generations, sound, save systems, AI, physics, multiplayer, online services, cross-play, procedural generation, open world, photoreal rendering and so on).
- **Company:** a training program and an R&D lab.
- **Lanes:** three upgrades for each other lane (site templates and your own CMS for websites, push notifications and A/B testing for apps, spam filters and recommendations for social, supply chain deals and battery safety for hardware, efficient training and safety evals for AI).

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
| Viral by accident | Random | Suggests a lane; picking it comes with a head start |

You pick a second lane and how to take it on, or decline:

- **Expand:** keep the first business and run both, with shared staff and cash and your attention split.
- **Pivot:** sell the first business, take the lump sum and your skills, and go all in. People whose skills carry over to the new lane come with you; the rest stay with the buyer.
- **Stay focused** (Android): turn it all down and keep running the one business. A second lane is still available later through expansion.

## Later expansion and the ecosystem company

- After the Pivot Point, once every lane you run has been profitable for 12 straight weeks, you can add another lane, up to three. If you stayed focused, that's how a second lane arrives, if you ever want one.
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
- **Rivals**, one main rival per lane (Electronic Arts, Squarespace, Google, Meta, Apple, OpenAI on Android; parody names in the Swift version), squeeze your demand, launch competing products and poach your people. The news also mentions the other big names in each lane.
- **Reputation carries across everything.** Scars (predatory monetization, flops, privacy breaches, scandals, recalls, antitrust, layoffs) fade at different speeds and make each lane's audience suspicious in different ways. Ship a predatory mobile game and your social app launches to suspicion.

## Endings

| Ending | How | Variants |
| --- | --- | --- |
| IPO | From year 5, a business worth $500M (excluding cash) with $50M annual revenue, and the board's sign-off | The Ecosystem IPO, IPO Whale-Funded, The Compute IPO, Bootstrapped to the Bell |
| Acquired | Accept a buyout | The Quick Exit (before the Pivot Point), Absorbed by a rival |
| Stayed independent | Reach year 15 | Still Indie, The Lifestyle Business, The Independent Empire, Private and Thriving, Zombie Unicorn |
| Bankrupt | Eight weeks in the red (twelve on Easy, six on Hard) | Burned the Compute Budget |
| Ousted | Board confidence hits zero | Fired From Your Own Company |

The ending screen tells the story of your run: where you started, what forced the Pivot Point and what you chose, the lanes you added, the crossovers you built, your record of hits, flops, recalls and scandals, the scars people still remember, how much you raised and kept, and the eras you lived through.

## Not built yet

- Multiple concurrent products per lane (a second app, two games in parallel).
- Rival companies as full simulations, plus acquisitions you make yourself.
- Sound, and hand-drawn art. (Everything is drawn in code: the isometric office with each office tier, its perks and its people, the staff portraits, and box art generated for every game from its topic, genre, platform and rating.)
- The studio update, the running clock, the removal of energy and the 0.5 gameplay update in the Swift (macOS and terminal) version.
- Steam-style achievements per ending variant.
- Proper tuning passes with real playtesters; the current numbers come from autopilot runs.
