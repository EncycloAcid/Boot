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
- **Scores.** Every game starts at 5.0, then each part adds or takes off points: **craft**, the design and tech points per unit of work against a benchmark that starts at 2 and rises 10% a year, on a log scale (-3 to +2; better staff talents and engines raise it); **topic and genre** (great +0.6, good +0.2, okay -0.3, bad -1.5; two genres average); **audience** (+0.2, or -0.8 for a bad fit, -0.4 if the platforms' players aren't that audience); **platforms** (+0.2 a good fit, -0.7 a bad one, -0.1 per extra platform); **focus** (2.5 points per unit of focus match above 0.6, -1.5 to +0.8); **design and tech balance** (+0.2 within five points of the genre's ideal, otherwise up to -1.0); **bugs** (up to -2.5) and **polish** (up to +0.4); a dated engine (up to -1.2) or a cutting-edge one (+0.2); +0.4 on trend; -0.8 for a retread and -0.6 for a sequel within a year; perfectionists (up to +0.4); -0.5 for falling 1.5 below your last three games' average and +0.2 for a new best; and the critics' mood (up to ±0.6). Above 8 each extra point counts half. The six outlets then review around that total with their own tastes. The verdict screen shows the biggest reasons and the game report every part.
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
- **The market.** Fifteen real studios (Electronic Arts, Ubisoft, Rockstar Games, Nintendo, PlayStation Studios, Xbox Game Studios, Valve, Capcom, FromSoftware, CD Projekt Red and others, each with its own typical quality) release games of their own, bigger as the years go by. Every game on sale, theirs and yours, competes on a weekly top-10 sales chart. Launching within six weeks of a rival's 7.5+ game of a similar size in the same genre costs you about 15% of your launch sales, and the game report says so.
- **Real franchises.** 47 real series release on their own schedules: yearly series (EA Sports FC, Madden, Call of Duty, Just Dance, Fortnite chapters) in the same week each year, the rest every few years with some drift (Grand Theft Auto every decade or so, Half-Life whenever it feels like it). Big games are announced six months ahead, smaller ones three, in the Coming soon list. A 9+ AAA release is a giant: launching within three weeks of one costs you 20% of your launch sales, and when one lands, anything released in the last six weeks loses a fifth of its sales.
- **Two teams.** Second-team research (110 RP, after large games, on a studio floor or bigger) adds a second development slot. While both teams are busy, the game staff split by a slider (Team 2 gets 20-80%, 40% by default); a team working alone gets everyone. Team 1 also does contracts and expansions. Team 2 can't make AAA games, and each game's minimum team size counts that team's own people. Everything else (focus, hype, campaigns, launches, reviews) works per game.
- **Buying studios.** All 29 studios on the market can be bought: 14 indies (tens of millions to $1.5B) and 15 publishers (billions), whose values drift with the era and the years like the league table's. You pay the value plus 15%; the big publishers only sell to companies three years old or listed. An owned studio keeps releasing games (and its real franchises) under its own name; they show as yours on the chart, never clash with your launches, and don't steal players when a giant lands. It pays about 6% of its value a year, scaled by its last year's hits and flops, its value counts towards yours, it leaves the league table, and it can be sold for 90% of its value.
- **Live-service games** (research after online services) are medium or bigger games that ship with a player base of six times their launch-week copies. They sell 15% less at launch and cost 25% more a week to make. Players spend each week (cosmetics only $0.22, fair $0.40, aggressive $0.75 a head) and leave at about 1.2% a week for a 7/10 game, more for weaker ones and up to 2.5× faster once 20 weeks pass without a season. A season (at least 8 weeks apart) costs the size's weekly budget plus 60 cents a player and brings back 18-30% more players. Servers cost $2,000 plus 3 cents a player a week. An aggressive store with 10,000+ players earns a pay-to-win scar each quarter. Shutting a game down with 20,000+ players still in it costs fan trust; games under 1,500 players shut down on their own.
- **Your own label.** Publishing research (40 RP, after marketing) brings pitches from small developers every few weeks: a game with a topic, genre, size and platforms, the funding they need (paid when you sign), how long it'll take and your share of what it earns after the platforms' cut (35-65%). How good the team really is stays hidden. The pitch shows the demo (rough, promising or polished) and, two times in three, their last game's score, both noisy reads of their talent; the topic and genre fit is the strongest signal, and the notebook tells you the fits you've learned. Up to three signed games at a time; you can buy them marketing while they're made, and now and then one slips a few weeks. A finished game gets six reviews like yours, goes on the chart, sells on its score, your fans (a smaller lift than your own games) and its hype, and its sales come in every week. Its developer keeps patches and sequels. Signing everything about breaks even; great combos return about twice their funding and bad ones lose most of it. Pitches go elsewhere after six weeks.
- **Remasters** (research after sequels) rebuild a game that scored 7 or more and is at least three years old, once. A remaster keeps the topic, genre and size, uses today's engine and platforms, takes half the work and starts with hype. Critics review it like any game; its launch sells 60% of a new game's for a 7/10 original, rising to 90% for a 10.
- **Esports** (research after online services) give a competitive hit a pro league: an action or strategy game of yours that scored 7.5 or more, built on an engine with multiplayer, once 100k people own or play it. Setting one up costs $100k plus 40 cents a copy sold. It starts with viewers worth 4% of the audience (more for higher scores); sponsors pay 90 cents a viewer a week, production costs $8,000 plus 15 cents a viewer, and viewers drift away slowly, faster once the game is off sale. While it runs the game's sales fade more slowly, a live game keeps more players, and fans grow. Every 26 weeks there's a championship with the prize pool you pick (modest $100k, major $1M, record $5M): it wins over 5%, 15% or 30% of the gap between the viewers and 40% of the audience, and gives sales a bump. A bigger pool only pays off on a big game. If you can't afford the pool it drops to one you can, or the final is called off. One final in 16 or so ends in a match-fixing scandal that costs 30% of the viewers. A league with under 2,000 viewers folds, and you can wind one up yourself.
- **Achievements**: 46 in four tiers, checked weekly and kept with the week they were earned. They pay nothing.
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

## Personalities

About 70% of candidates come with a trait, shown before you hire them, and keep it: **Perfectionist** (90% output, +1.5 quality points each, up to +6 for a team), **Hustler** (115% output, morale settles 6 points lower), **Night owl** (130% output and no morale loss during crunch), **Mentor** (everyone else on their team earns 25% more experience), **Diva** (125% output, morale settles 12 points lower, twice as likely to quit when unhappy), **Team player** (lifts teammates' morale by 4 points, up to 8), **Rainmaker** (+3% demand for their business, up to +8%), **Loyal** (never quits, rivals don't poach them, 3% raises on level-up instead of 6%) and **Fast learner** (double experience).

## Product roadmaps

Every business except the game studio has four features to build, each with a cash cost and a build time in weeks. While one is being built the team gets 15% less done; once it's live the effect is permanent. One build per business at a time.

- **Web agency:** case studies (+5% demand), design system (+6% output), retainer packages (+3% demand, -5% costs), offshore partners (+12% output, -4% demand).
- **Apps:** guided onboarding (-4% churn), dark mode and polish (+3 quality), referral program (+6% demand, +3% costs), offline mode (-5% churn, +5% costs).
- **Social:** stories (+3% growth, more toxicity), algorithmic feed (+8% demand, more toxicity), community notes (toxicity falls faster), live video (+4% growth, +6% costs).
- **Hardware:** drop-test lab (-30% recall risk), in-house chip (+4 quality, -3% costs), retail partners (+6% demand), repair program (+2 quality, -15% risk).
- **AI:** inference optimization (-5% compute), data deals (+5% capability per run, +10% regulator risk), red team (-15% risk), enterprise API (+4% demand).

The effects are small on purpose: churn and growth compound every week.

## Hall of Fame and legacy

Every run that ends goes into a Hall of Fame kept across games (a run that ends, plays on as a public company and ends again keeps one entry), ranked by valuation. Six legacy perks follow you into new companies once earned: **Been here before** (any finished run: +$10k), **Serial founder** (five runs: +$15k and 10 RP), **IPO veteran** (an IPO: a star candidate on day one), **Survivor** (a bankruptcy: experience in your starting lane), **Ecosystem builder** (an ecosystem ending: 20 RP) and **Billion-dollar founder** (a $1B run: the espresso machine from day one).

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

**Eras.** Twelve eras (the Bootstrap Years, then the AI Gold Rush, VR Hype Cycle, Crypto Winter, Remote Work Boom, Privacy Panic, Creator Economy, Retro Revival, Metaverse Hangover, Chip Shortage, Cloud Gaming Push and Super-App Era) each last 78-130 weeks and change demand per lane, salaries, GPU prices, hardware part costs and investor appetite. Each has a life: the first 20% is its dawn, when its effects build from 35% to full strength; then its peak; and the last 25% it fades, easing to half strength. When it starts to fade, the next era is chosen (unseen ones first) and its rumours appear in the news and on the era card. The company page shows the era, its phase and strength, every effect as it stands, and a timeline.

**The news.** The Daily Byte has a front page (the era card, a lead story from the last six months, and the latest stories) and four desks: your company, industry, games and markets, plus your company log. Stories come from outlets that would run them (The Economist and Bloomberg for the era and the markets, TechCrunch for funding, IGN and Kotaku for games, The Verge and Wired for devices and AI). Sources: the era's dawn, peak and fade headlines every few weeks, the start of each era and rumours of the next, every rival's big moves in any lane, notable rival game releases (8.5+, mega releases and AAA flops), Game of the Year, esports finals, the quarterly markets (the most valuable company, hot and cold sectors, your place in the top twenty), and your own reviews, funding rounds, IPO, acquisitions, label hits, milestones and ending. The paper keeps the last 300 stories. Each desk groups its stories by age (this week, this month, the last three months, this past year, older); the front page's lead is the biggest story of the last six months other than the era's own, which the era card already tells; and era stories carry a line on who the era is good and bad for.

## Decades

A run can start today or at one of tech's revolutions, and play through to the present (the run ends at the start of 2027, the year a run started today begins in, rather than after fifteen years):

| Start | Opens in | Lanes open |
| --- | --- | --- |
| Today | The Bootstrap Years | All six |
| 1977: Home computers | The Home Computer Revolution | Games, Hardware |
| 1985: The console wars | The 8-Bit Console Wars | Games, Hardware |
| 1995: The web | The Dot-com Boom | Games, Hardware, Websites |
| 2007: The smartphone | The Smartphone Revolution | Games, Hardware, Websites, Social (Apps from 2008) |

**Calendar.** Dates read as real months and years. Week 0 of a run started today is the start of 2027, and every platform launch, retirement and real series release sits on that shared timeline, so a run started in 1985 sees the same world a run started today does once it gets there.

**History's eras.** The Home Computer Revolution (1977), the Video Game Crash (1983), the 8-Bit Console Wars (1985), the Multimedia Age (1990), the Dot-com Boom (1995), the Dot-com Bust (2000), Web 2.0 (2004), the Smartphone Revolution (2007), the Free-to-Play Boom (2012), then the VR Hype Cycle (2016), the Creator Economy (2018), the Remote Work Boom (2020), Crypto Winter (2022) and the AI Gold Rush (2023). Each lasts until the next begins and has a dawn, peak and fade like any era. The dot-com boom brings investor appetite of 1.8×, and the bust that follows 0.45×. The historical eras never come round in a run started today. After 2027 (playing on), eras are picked as usual.

**Businesses arrive** when they became possible: games 1970, hardware 1975, websites 1994, social media 2004, apps 2008, AI 2012. Until then they can't be picked at the start, at the Pivot Point or as an expansion.

**Platforms.** The Atari 2600, Commodore 64, NES, Game Boy, Sega Genesis, Super Nintendo, PlayStation, Nintendo 64, PlayStation 2, Xbox, GameCube, Nintendo DS, Xbox 360, Wii, PlayStation 3, PlayStation 4 and Xbox One each launch and retire in their real years. The PC is there from the start (the Apple II and its rivals, then the IBM PC and clones, then Steam) and grows from about 30% of today's players in 1977. Phones arrive with the App Store in 2008.

**The games market.** The 47 real series debut in their real years. Their entries before today come out in their real years with their real titles (from Super Mario Bros. in 1985 to Grand Theft Auto VI in 2026); after that they carry on with the entries a run started today would see. Rival games are capped by year: small only before 1990, medium before 1998, large before 2005. Studios appear from their founding year until they closed, under the names of the day (Activision, Square, Namco, Sony Computer Entertainment, Microsoft Game Studios, Epic MegaGames), and Atari, Sierra On-Line, Infocom, Origin Systems, Westwood Studios, LucasArts, Sega, Konami and id Software join the market. Studios are worth far less in the past (values shrink about 14% for each year before 2027), so buying EA in 1987 is a bargain.

**Reviews and the news.** Reviewers are the ones of the day: Creative Computing (to 1985), Byte (to 1998), Electronic Games (1981-85), Computer Gaming World (1981-2006, fond of strategy, simulation and RPGs), Zzap!64 (1985-92, in wildly swinging percentages), Nintendo Power (1988-2012, generous to Nintendo platforms) and Electronic Gaming Monthly (1989-2014), then IGN, GameSpot, Game Informer, Eurogamer, PC Gamer and Edge as they open. News outlets that didn't exist yet are swapped for ones that did. Each lane's main rival changes over the years (games: Atari, Nintendo from 1985, EA from 2000; social: MySpace, Facebook, Meta; hardware: Commodore, IBM, Dell, Apple; AI: Google DeepMind, then OpenAI), with a story when the giant changes. The league table includes only companies that existed, under their names of the time (Google, Facebook, Snapchat), with today's giants scaled back, plus IBM, Atari, Commodore, AOL, Yahoo, Nokia and BlackBerry at their real values through the years.

**Balance.** With the autopilot, historical starts land in the same range as a run started today (median peak valuations of $650M-$1.2B). The exceptions are hardware in 1995, which rides the dot-com bubble, and social and hardware in 2007, which spend the smartphone years before any bad era and peak at $3-4B.

## Research through time

Every research item has the year it became possible (from 1978 for sequels, procedural generation and basic AI to 2023 for photoreal rendering, neural NPCs and AI site builders). A run started today has them all; a run set in the past can't research anything before its year. Twenty era items can only be researched while their era is on (one for each era but the Bootstrap Years) and stay for good: their effects are lane modifiers (demand, costs, output, quality, risk, compute) or, for full-motion video, VR support and cloud streaming, engine features worth two levels.

Reviewers' expectations (craft points per unit of work, and the engine level they expect) rise for the first fifteen years of a run and then hold, so a fifty-year Decades run stays playable.

## Era challenges, moments and reviews

Each era but the Bootstrap Years has a challenge, picked from its preferred kinds by what suits the company (a hit needs a game studio; mastering the tech needs research you could use): earn 1.2 times last year's revenue over the era, reach 2.5 times your valuation, 26 profitable weeks in a row, ship a game that scores 8 or more, research the era's own tech, or grow the team by half. Meeting it pays six weeks of company revenue (at least $50k), 25 RP and 3 reputation.

When an era reaches its peak, its moment comes round once: a decision with effects for the rest of the era or longer, mostly as demand, output or cost changes on your businesses for 13 to 52 weeks. Game trends follow the era three times in four. When an era ends, its span in the timeline records what you earned, your value at the start and end, and whether you met the challenge, and the paper runs a review.

## Layout: sideways or upright

The Android app plays sideways by default. On a phone held sideways it borrows Game Dev Tycoon's use of space: the office fills the screen, small readouts float in the corners (menu, company and date top left; research points, fans or reputation, cash and weekly profit top right), the game in development runs across the top centre with its design, tech and bug points under it (or a prompt to open Today when nothing is in development), the desk count sits bottom left, and one dock along the bottom, the full width of the screen, holds the tabs (Today, Lanes, Research, Team, Company, News) spaced evenly and the clock (speed, week, play) at the right. Tabs open as a window in the middle with the office dimmed behind, running the full height from the top of the screen to the dock: the corner cards and the desk chip step aside for it, and its title bar carries the date and your cash. Tapping outside or Back closes it, and the dock and clock stay usable. Sideways the game runs full screen, with the phone's status and navigation bars hidden until you swipe from an edge. Tapping someone in the office opens their card beside them (level, role, trait, what they're working on, design, tech and research, morale, experience, pay, and a way into the Team tab; the founder's shows how their attention is split), with a marker over their head; other taps on the office do nothing, so a stray touch never changes screens. Today opens by itself when a lesson, inbox item or urgent tip arrives, not every week. Because a phone held sideways is short, that layout draws the whole UI at 94% scale (system font size still applies on top), keeps 16dp from the screen edges, and switches the shared components into a compact mode (`LocalCompact`): somewhat tighter section, card, row and tile spacing, paired stat tiles on one row, one-line staff and candidate rows, the company valuation beside its stats, and event choices side by side. Dialogs and sheets open in their own windows, so they re-apply the same scale through `GameDialog` and `Scaled`. Windows start just under the measured corner readouts, so large text never overlaps them. Tablets and unlocked screens use the floating layout. Upright is the classic layout: a header with the clock and your numbers, one scrolling page per tab with the office on the home page, and a tab bar at the bottom. The choice is made on the new game screen or from the menu, is remembered between runs, and locks the screen's rotation to match.

## Tutorial and How to play

A career can start with the tutorial (on by default when the Hall of Fame is empty). Rosa, a mentor, gives seven lessons on the office screen: welcome, make your first thing (by business: start a game, take a contract, start a device design or a training run; for apps and social, a look at the build), let time run, hire, read the money, spend research points, and a sign-off. Lessons tick themselves off when the game sees you've done the thing (in any order), the reading-only ones when their button is pressed; each button goes where the lesson points. While the tutorial runs, the Next step card only appears for money trouble. How to play, in the menu, has fifteen pages and can restart the lessons.

## Scenarios and sandbox

Besides a career there are two more ways to play, picked under How to play on the new game screen.

A **scenario** is a fixed start, an objective and a deadline. Meet the objective in the first half of the time for gold, by three quarters for silver, or by the deadline for bronze. When the scenario is decided (won, or out of time) a decision asks whether to keep playing or end the run there. The Hall of Fame records the scenario and medal, and the picker shows your best medal in each.

| Scenario | Business | Starts | Objective | Time |
| --- | --- | --- | --- | --- |
| Garage to Glory | Games | Today | Ship a game that scores 9 | 5 years |
| Cult Following | Games | 1977 | 500k fans | 10 years |
| The Crash of '83 | Games | 1983, $60k | Be worth $50M | 6 years |
| Console Kings | Hardware | 1985 | Sell 1M devices | 8 years |
| Dot-com Gold Rush | Websites | 1995 | Be worth $200M | 5 years |
| Survive the Bust | Websites | 2000, $300k, three staff, standing and two retainers | 26 profitable weeks in a row | 3 years |
| The Social Network | Social | 2004 | 20M people | 6 years |
| There's an App for That | Apps | 2008 | 500k app users | 4 years |
| Whale Watching | Games | 2012 | Earn $20M in a year | 6 years |
| Frontier Lab | AI | Today | Capability 100 | 8 years |
| Giant Slayer | Games | Today | Drive your rival out of the market | 8 years |
| Winter Is Here | Websites | Today, in crypto winter | 52 profitable weeks in a row | 3 years |

A **sandbox** run starts with $1B and can't go bankrupt (cash can go negative). It can start at any point in history like a career, but it earns no achievements and stays out of the Hall of Fame.

Era moments are priced at a quarter of the last four weeks' revenue (at least $5k), so a small company can afford them, and they wait until an era is at least half a year old.

## Rivals

Each lane has a main rival with a strength (their market muscle, which also sets how much of the market they take from you) and a heat: how much they want to hurt you. Heat rises each week you run the lane, faster the more you earn relative to the lane's scale (Games $300k a week, Websites $60k, Apps $100k, Social $150k, Hardware $600k, AI $400k), and cools slowly. Calm below 0.3, wary to 0.6, hostile above.

From week 30, once you have something on sale, a rival can move at most every 16 weeks, more likely the hotter, stronger and more aggressive they are. Moves suit the market (price wars for agencies and devices, copycats for apps and social, lawsuits for hardware and AI, exclusive deals for games):

| Move | Choices |
| --- | --- |
| Copycat | Rush out something better (3 weeks' revenue; -5% demand for 12 weeks), outspend them (1.5 weeks'; -8% for 16), or ignore it (-15% for 26) |
| Price war | Match (-10% for 13 weeks, and they lose strength), hold (-20% for 17), or go upmarket (-12% for 17, +2 reputation) |
| Ad blitz | Counter (2 weeks' revenue; -3% for 6) or ride it out (-12% for 10) |
| Talent raid | Match their offers (30% raises), offer equity (0.4% each) or let your two best people go |
| Lawsuit | Settle (6 weeks' revenue) or fight (1.5 weeks' in fees; 55% you win costs back and they lose strength, otherwise 9 weeks' in damages and an injunction) |
| Exclusive deal | Outbid them (4 weeks' revenue) or -10% for 26 weeks |
| Smear | Answer it (usually works) or stay above it (-4 reputation) |

Your counter-moves, each with a 13-week cooldown per rival: **undercut** (+15% demand and 12% higher costs for 13 weeks), **poach a star** (a skill 8-10 hire at a premium) and **attack ads** (+5% demand for 8 weeks, -1 reputation). All hit the rival's strength and raise their heat. A rival pushed below 12% strength retreats for a year.

## The inbox and consequences

Decisions now come in two kinds. Crises, people and rivals' moves stop the clock as before. Opportunities go to the inbox, wait 3 to 6 weeks and don't stop the clock; ignoring one takes the last choice. Some decisions schedule consequences: a quiet security patch leaks 45% of the time (a fine of 6 weeks' revenue and two scars), someone who pushes through burnout quits half the time, a star refused equity often leaves for the rival, a lawsuit is decided in 13 to 26 weeks, a client taken to court pays 70% of what they owed 60% of the time.

## Deeper businesses

- **Agency.** A client who finishes happy (satisfaction over 75%) offers a retainer 30% of the time: 5% of the project's value a week, taking about 1.15 points of work a week per $400. Retainers come before projects; fall well behind and they leave. Agencies with standing get invitations to pitch for big-brand projects ($120k-$600k): the pitch costs 6% of the value, and the odds rise with standing.
- **Apps.** A price level (cheap, standard, premium) trades installs and churn against revenue per user. Six experiments, one at a time and each once, run for four weeks on a slice of users and report a lift on installs, churn or revenue per user that can be negative; roll it out or back.
- **Social.** A creator fund (+4% or +8% growth, paid per user, a little more mess), a premium tier (3% of real users at $0.60 a week once mature, slightly slower growth, fewer bots) and house rules: open (+4% growth, more toxicity, advertisers pay 15% less), balanced, or strict (-5% growth, less toxicity, +12% from advertisers).
- **Hardware.** The device on sale can be priced 20% under to 20% over list; demand moves with the price to the power -2.2. A livestream ($50k) or keynote ($400k) booked during design adds launch hype.
- **AI.** A finished training run waits for your call: ship it now, safety-test it for six weeks (regulator heat -0.25, +2 reputation, 10% more capability), or release the weights (+4 reputation, +20 RP, less regulator attention; subscribers -10% and API demand -40%, enterprise +15%). GPUs can be reserved for a year at 40% under rent, used first after your own.

## Endings

| Ending | How | Variants |
| --- | --- | --- |
| IPO | From year 5, a business worth $500M (excluding cash) with $50M annual revenue, and the board's sign-off | The Ecosystem IPO, IPO Whale-Funded, The Compute IPO, Bootstrapped to the Bell |
| Acquired | Accept a buyout | The Quick Exit (before the Pivot Point), Absorbed by a rival |
| Stayed independent | Reach year 15 (or the present, in a Decades run) | Still Indie, The Lifestyle Business, The Independent Empire, Private and Thriving, Zombie Unicorn |
| Bankrupt | Eight weeks in the red (twelve on Easy, six on Hard) | Burned the Compute Budget |
| Ousted | Board confidence hits zero | Fired From Your Own Company |

The ending screen tells the story of your run: where you started, what forced the Pivot Point and what you chose, the lanes you added, the crossovers you built, your record of hits, flops, recalls and scandals, the scars people still remember, how much you raised and kept, and the eras you lived through.

## The trade show

Week 26 of every year brings the big games show, named for its era: the Consumer Electronics Show before 1995, E3 until 2020, Summer Game Fest after. It's a decision for any studio with a game lane.

| Booth | Cost today | Crowd | Hype | Fans | Needs |
|---|---|---|---|---|---|
| Small | $15k | 1× | 8 | 2% | Nothing |
| Medium | $90k | 3× | 18 | 4.5% | Nothing |
| Large | $400k | 8× | 32 | 8% | 50k fans |

Costs follow the same past-price scaling as everything else, so an 1985 medium booth costs about $12k. What the crowd makes of the booth (its appeal, about 0.1 to 1.5) comes from the game's launch forecast, scaled down for a game less than half done, with 15% more for being on trend and 20% more for the sequel to an 8+ hit. Hype gained is the booth's hype × (0.4 + appeal), capped at 100 on the project; new fans are the booth's share of your fanbase × (0.5 + appeal), at least 400 per crowd unit. Visitors are 6,000 per crowd unit × (0.4 + appeal), give or take.

- **Too early.** A game under 25% complete on a medium or large booth gets half the hype and a "too early to judge" write-up.
- **Crashed demo.** A buggy build (bugs over a quarter of its design and tech) on the main stage crashes 40% of the time: 30% of the hype, a reputation knock, and no award.
- **Best in Show.** A game forecast at 8 or more with appeal of 0.85 or more wins 60% of the time from a large booth and 25% from a medium one: 15 more hype, 25% more fans, +3 reputation, and the Best in Show achievement.
- **Back catalogue.** With nothing in development the booth shows your old games: no hype, 60% of the fans, appeal from your best game of the last two years.
- Medium and large booths lift the team's morale a little; a large booth that doesn't crash adds 1 reputation.

A report follows straight away with the visitors, hype and new fans, and Today's Game studio card counts the show down from eight weeks out. Hype decays 1% a week, so a show is worth most a few months before launch. The autopilot books a large booth with a promising game and four times its cost in the bank, a medium one with any game and three times the cost, and a small one otherwise when it can afford four.

## The show season

Around the trade show the games lane has a calendar of stages (`Shows.kt`). Week numbers are weeks of the year.

| Week | Show | From | What happens |
|---|---|---|---|
| 7 | D.I.C.E. Awards | 1998 | Game design; best indie game |
| 11 | Game Developers Choice Awards | 2001 | Best technology; audience award |
| 15 | BAFTA Games Awards | 2004 | Artistic achievement; best game |
| 22 / 24 / 25 | State of Play (2019+), Xbox Games Showcase (2020+), E3 press conferences (Sony 1995–2018, Microsoft 2001–2019, Sega 1995–2000) | | Console makers invite promising games |
| 26 | The trade show | | Booths and keynotes (see above) |
| 34 | Gamescom (2009+), Opening Night Live (2019+); Nintendo Space World (1989–2001) | | World premieres; Nintendo invites |
| 36 | Your own showcase | 200k fans | Stream your games |
| 37 | Nintendo Direct | 2011 | Nintendo invites |
| 46 | Golden Joystick Awards | 2003 | Fan-voted game of the year; best indie game |
| 49–50 | The year-end show | | World premieres (2003+); Game of the Year and best independent game |

The year-end show is the Arkie Awards before 1983, the Golden Joystick Awards until 2003, the Spike Video Game Awards until 2014 and The Game Awards after.

**Prizes.** Every prize considers games released in the last 52 weeks that scored 7 or more, yours and rivals'. Its jury rates each game by score, leaning its own way: design takes the score as it is, technology adds 0.6 for AAA and 0.3 for large games, art adds 0.5 for small and 0.3 for medium ones, and the fan vote mixes the score with the log of lifetime sales. Indie prizes only consider small and medium games. The top five are nominated, and the winner is drawn with weight e^(3 × (rating − best rating)), so the favourite usually but not always wins. A win: fans +8% (at least 1,000), reputation +2, the game's weekly sales +25%. Award shows never stop the clock; nominations and results come as news.

**Reveals.** A trailer's hype is its stage's base × (0.4 + appeal) × a reveal boost of 1.5 for the first trailer (the announcement, which makes the news), 1.0 for the second, 0.75 for the third and 0.55 after; it also finds fans. Appeal is the trade show's measure. Trailers cost by game size ($10k small to $200k AAA, scaled for the year). Bases: 14 at a console showcase, 22 at a world premiere, 30 at your own showcase. The trade show booth counts as a reveal too.

- **Console showcases.** A maker invites the first of your games in development that's on one of its consoles, if its appeal is at least 0.45; otherwise it passes, and says so. Options: show a trailer; take the marketing deal (the maker pays $60k to $1.2M by size, the trailer gets 35% more hype, fan trust drops 0.03, once per game); shadow-drop it if it's finished; or not this time.
- **World premieres.** Gamescom ($150k slot) and the year-end show ($250k slot) offer one to your game in development.
- **Shadow drops.** A finished game can be released live at any of these stages: +20 hype, then it launches.
- **Your own showcase** costs $300k. If your best game's appeal is under 0.45, viewers leave disappointed: half the hype and fan trust −0.02. Otherwise every game in development gets a trailer and fans grow by 5% × (0.5 + appeal).
- **Keynote stage** at the trade show: $1.2M with 250k fans, a crowd of 15, 45 hype, 12% fans, Best in Show 70% of the time when it qualifies, +2 reputation.

The autopilot shadow-drops what's finished, takes marketing deals when cash is under $1M, shows trailers when it has ten times the cost, premieres games with appeal 0.6 or more when it has five times the cost, and streams its own showcase when it can afford five times the cost. Over a 15-year career it wins four to nine prizes.

## The founder

On the new game screen you make the founder: skin tone, hair colour and style, clothes and their colour, glasses and a beard, with a live portrait and a shuffle button. The look is stored in the save as positions in the game's palettes (`Appearance`), so the office, the Team tab, portraits and the founder's card all show it; until you change something it follows the look your name and company would give you. Staff looks are still drawn from their ids.

The founder takes one special trait, or none:

| Trait | Effect |
|---|---|
| Coder | The founder's own output × 1.4 |
| Visionary | +2 to the founder's design in the studio's average |
| People person | Staff morale settles 0.08 higher |
| Showrunner | Hype from the trade show, trailers and showcases × 1.3 |
| Tinkerer | Research points × 1.25 |
| Bootstrapper | Starting cash × 1.6 (not in a sandbox) |
| Media darling | Reputation gains × 1.3, losses ÷ 1.3 |

## Wishlists and the release calendar

Each game in development keeps a wishlist count, its release week (if one is set) and how many times it has slipped. Nothing is wishlisted until the game is announced. Every trailer (a show booth, a console maker's show, a premiere, your own showcase) adds `hype gained × (150 + fans × 0.03)`; after that, each week adds `hype × (8 + fans × 0.0008)`, a quarter more with a release date set, and 0.4% of the list fades.

At launch, `wishlists × conversion × price × price-tier factor` (in that era's money) comes in once, on top of the usual launch week, where conversion is `(score − 4) / 30` between 2% and 20%. It isn't part of the weekly sales that tail off, and it's listed in the game report. Two windows now stack on the seasons for every genre: the **holiday rush** (weeks 46–51, ×1.2) and the **January slump** (weeks 1–4, ×0.9).

**The calendar** shows the next 26 weeks for the game in development, each with its expected launch lift: season × window × clash, where the clash uses the same rules as launch day applied to the announced rival releases (a must-play release up to three weeks before costs 20%; a big game in your genre up to six weeks before, 15%). Shows and must-play releases are marked, and weeks before the expected finish are dimmed with a warning. Setting a date on an announced game makes the news. On the date, a finished game launches by itself (the clock doesn't stop for "ready to ship" when there's a date). An unfinished one raises a decision: delay four weeks (fan trust −0.02, wishlists −8%) or until it's done (no date; −0.03, −12%), each reported in the news. The autopilot doesn't set dates and takes the four-week delay.

## Founder levels and skills

The founder earns experience: 1 a week, 10 per release (any lane), 15 more for a hit and 4 for a flop (you learn from it). Level n needs `40 + 20n` to reach the next, up to level 10. Every level brings a skill point; at level 5 you pick a second trait (any but your first, and not Bootstrapper, which only counts on day one). Both traits apply.

| Branch | Tier 1 | Tier 2 | Tier 3 |
|---|---|---|---|
| Craft | Hands-on: your output × 1.2 | Eye for design: +1 to your design | Craftsmanship: +1 quality in every lane |
| People | Mentor: staff XP × 1.2 | Steady hand: morale settles 4 points higher | Inspiring: quit chance halved; kinder story outcomes |
| Business | Pitch: hype × 1.15 | Networker: reputation gains × 1.15, losses ÷ 1.15 | Dealmaker: demand × 1.04 in every lane |

Each branch is learned in order. The autopilot spends points tier by tier and takes People person, Showrunner or Coder as its second trait.

## Staff stories

About 3% of weeks (from week 12, with at most two stories under way, and one per person) start a story for people who fit it. Each chapter is a decision that stops the clock.

- **Dream team** (two people on the same team in different roles, both content): paired, each gives 15% more while they share a team; 16–26 weeks later they ask to lead together (10% raise each; 25% more from each and +1 quality in their lane, for as long as both stay) or are refused (morale −0.15).
- **Name on the box** (a skill 7+ designer or engineer on the game team, after your first release, while a game is in development): credited, the next release decides it. At 7 or better: creative director (25% raise, +1.5 game quality, fans +3%, in the news) or keep them where they are (a rival tries to poach them). Below 7: stand by them (+morale) or take the name off (−morale).
- **Running on empty** (skill 4+, morale under 0.65 or in crunch): home on time (−10% for six weeks, then +morale) or let them work (+15%, morale −0.01 a week); after 5–9 weeks, four weeks off (back with morale 0.85 and half a level's XP) or push through (half quit within weeks, a quarter with Inspiring; Loyal people stay).
- **Under their wing** (a level 5+ veteran and a level 1–2 newcomer on the same team): official, the newcomer learns twice as fast and the veteran gives 10% less until the newcomer is two levels up (or 40 weeks pass).

Random burnout skips people already in a story. The autopilot takes the first choice of each chapter.

## Show night

**Award nights.** Each prize at an award show is decided as before (five nominees, a jury weighted towards its taste), and now returns its category: the nominees, with covers, and the winner. If any category has a game of yours in it, those categories become an award night (`AwardNight`), shown as a ceremony that opens one envelope every 1.6 seconds (or all at once with Skip). With a win, the speech is a choice: thank the team (+0.08 morale and 5 XP each), tease what's next (+8 hype, ×Showrunner/Pitch, with its wishlists, or the game's announcement if it hasn't had one) or thank the fans (+3% fans, +0.02 fan trust). At the year-end show the indie prize is held until Game of the Year is read out, after the fly-out decision.

**Stage reports.** Every trailer on someone else's stage or your own stream ends in a `ShowResults` report with the audience (2M for a console maker's show, 2.5M with its marketing deal, 6M for a world premiere, four times your fans for your own showcase, a twelfth of that before 2005), hype, wishlists and fans gained, the crowd's reaction (appeal ÷ 1.3, between 0.05 and 1: cold, warm, hot, on fire), two press quotes and four chat lines. The trade show's report has all of these too.

**Show moments.**
- A streamer preview: for an announced game at least half done with 15+ hype, about one week in 125. Sending a build adds 8 hype and 15% more wishlists. With 5 or more bugs it can crash live instead: a chance of bugs ÷ 20, at most 70%. That costs 5 hype and a point of reputation.
- A fan-vote campaign: the week before a show with a public vote (GDC's Audience Award and the Golden Joysticks), when one of your games is among the top eight. It costs $40k in today's money and adds 0.6 to your game's rating in that vote.
- An invitation to present at the year-end show: from 60 reputation, half the time. It gives +2 reputation, +1% fans and +4 hype for an announced game.

**Event art.** A decision about people (poaching, raises, burnout, clashes, equity, side projects, staff stories) shows their portraits; one about a show draws the stage with its name.

## Reviews and the press

Each review has a pull quote and, at the top, the outlet's own seal:
- Editor's Choice from IGN, GameSpot and PC Gamer, and Game Informer's Editors' Choice, at 9 or more;
- Eurogamer's Essential at 9 or more and Recommended at 8 or more;
- a Zzap!64 Sizzler at 9 and a Gold Medal at 9.5;
- Edge's 10 at 9.5;
- EGM's Gold Award at 9;
- Game of the Month from the others at 9.5.

Comments sometimes name the game and add a second point. Three player reviews follow the players' score (three thumbs up from 8.5, two from 7, one from 5). All this wording, along with news quotes, comments and rumours, draws on its own dice, seeded from the game and the week. Writing never changes what happens.

News stories carry a tag, an optional quote and speaker, a game cover, and from 1997 a comment count (more for your own stories as your fanbase grows) with a top comment. Launch stories carry the critics' roundup and the most quotable review. Awards, announcements, delays, shadow drops, chart-toppers and rival releases carry quotes and covers. A charts story runs every four weeks, and a rumour runs the week before each show on your calendar.

## Buying a company

A studio's owners have a stance, settled per studio and year: open to offers, needs convincing, or not for sale. Indies lean open; a studio whose last two years were poor leans open too, and so does a buyer with a good reputation. Nintendo and Valve are never for sale. Every studio also has a finding, fixed for the run: a hidden gem (2 in 10), talent ready to leave (2 in 10), a burnt-out team (1 in 10), past its best (1 in 10), or clean.

Due diligence costs 0.5% of the studio's value, at least $50k in today's money, and reveals the finding. An offer is the studio's value × (1 + premium), with a premium from −10% to +80%. The chance of a yes is a base plus twice the distance between your premium and the premium the owners think fair, plus (reputation − 50) / 200, between 2% and 95%:

| Stance | Base | Fair premium |
|---|---|---|
| Open | 0.75 | +10% |
| Wary | 0.45 | +25% |
| Closed | 0.15 | +50% |

Paying half in shares (listed companies only) dilutes the founder's stake by the share price's portion of the enlarged company.

Owners answer after two weeks:
- **Yes:** for a publisher, 30% of the time a rival (Tencent, Sony, Microsoft, Embracer, Savvy) bids 8–20% more, and you can outbid them or let it go (no calls for a year).
- **Counter-offer:** when the roll misses by less than 0.25, the owners ask for 10–25% more.
- **No:** otherwise, and they take no calls for six months.

Deals of $20B or more go to the regulators 40% of the time; their conditions cost 3% more. Closing asks how to run the studio:

| Plan | Profit | What else |
|---|---|---|
| Leave them be | ×1.0 | |
| Fold them in | ×0.6 | Three of their people (skill 6–10 by studio quality) join your game team, once |
| Cut costs | ×1.4 | −2 reputation and a news story; their games score 0.8 lower; the studio loses value over time, down to 75% |

The finding then plays out:
- **Burnt out:** −20% profit for a year.
- **Talent ready to leave:** −20% profit and −15% value unless left alone.
- **Hidden gem:** +30% profit in the first year, +10% after, and +15% value after six months.
- **Past its best:** loses 5% of its value every six months, down to 70%.

Plans change once a year. The old one-step `acquire` remains for tests and tools.

## Stages

Each show draws a stage after the real one (`StageLook`, picked by name): the shows' colours, the shape of the set (LED walls, curtains, a ring of light, a ballroom, a conference screen, an expo hall, a festival roof, a white studio set), lighting rigs and beams, the floor and the front rows. No logos or real trophies are drawn; winners get a plain statuette. The award night opens on the stage, with the categories over the dimmed house; banners on decisions use the same stages.

## Box art

Covers add a backlight halo and ground shadow behind the hero, a film grade (the scene's light screened in, the sky and ground multiplied over), grain, and the studio's initials on the platform band. Logos are lettered by genre and topic: serif gold (fantasy, RPG, adventure, strategy), monospaced neon (sci-fi, cyberpunk, hacking, AI, robots, space, time travel), serif bone-to-blood (horror), candy with a white outline (casual) and chrome italic (everything else). Fill passes set their draw style explicitly; a stroke pass otherwise carried over and left thin faces hollow.

## Save slots

The Android app keeps three save slots. Slot 1 is the original `autosave.json`, so a game from before slots carries on in it; slots 2 and 3 are `autosave-2.json` and `autosave-3.json`. Every change autosaves to the slot in play, in the background, and a one-line summary (company, date, cash, valuation, mode, when it was saved, whether it's finished) is stored beside it in preferences so the title screen can list the slots without decoding each game; a save from before slots is read once to describe it. Moving between slots writes the game in hand first, so a background write can never land in the wrong slot.

The title screen lists the slots with Continue and Delete (with a confirmation), and an empty slot can be tapped to take the next new game; when the slot last played is full, the next empty one is picked for you, and starting over a full slot asks before replacing it. "Save slots" in the in-game menu saves and returns to the title screen without ending the run, and the app reopens the slot last played. Abandoning a run clears its slot. The Hall of Fame and legacy perks are shared across slots.

## Sound and music

There are no audio files. A small synthesiser in the Android app (`audio/Synth.kt`) draws everything from oscillators: anti-aliased square and saw waves, triangle, sine, two-operator FM and noise, with envelopes, a one-pole filter, synthesised drums, and echoes that wrap round so a loop has no seam.

**Music.** Each era maps to one of six styles, and its tune is composed from a seed of its own, so the same era always plays the same tune and two eras in one style still differ. A track is two sections of 8 or 16 bars over two chord progressions picked for the mode (major, natural minor or dorian), with a bass line, a chord part and a melody built from two-bar motifs that land on chord tones on the strong beats, answer themselves over the next chords, and come home to the root.

| Style | Eras | Sound |
|---|---|---|
| Chiptune | Home computers, video game crash, retro revival | Pulse-wave lead, 16th-note arpeggios, triangle bass, 138 bpm |
| 16-bit | Console wars, multimedia | FM lead and brass stabs, slap FM bass, dorian, 116 bpm |
| House | Dot-com boom, Web 2.0, AI gold rush | Four on the floor, off-beat bass and chord stabs, minor, 122 bpm |
| Downbeat | Dot-com bust, crypto winter, privacy panic, chip shortage | Soft pads and electric piano, half-time drums, minor, 86 bpm |
| Lo-fi | Bootstrap years, remote work, creator economy, smartphones, free-to-play | Swung electric piano with sevenths, tape wobble, crackle, 80 bpm |
| Synthwave | VR hype, metaverse hangover, cloud gaming, super apps | Detuned saw pads and lead, echoing arpeggios, big snare, minor, 100 bpm |

The calmer styles leave the first section to the band and bring the tune in for the second. A track renders in about half a second on a background thread and loops as a stream; switching eras fades one out and the next in.

**Effects** come from comparing the game before and after each change (`audio/Cues.kt`), one sound at most, most important first: an ending sting, the launch fanfare, a new era, a bigger office, an achievement or goal, a decision waiting, the account going red, a game ready to ship, research done, a hire, and a coin for money moved by the player's own hand (never for the weekly ins and outs). Reviews ding one by one as they appear, pitched up a major scale with the score, then a cheer for an 8 or better or a sad trombone under 5. Buttons and tabs click quietly; a failed action buzzes.

**Settings.** Music and effects volumes (music 55% and effects 80% to start) are on the new game screen and under "Sound and music" in the menu, and are remembered. Sliders follow a squared curve so each step sounds even. Music pauses in the background and stays off if another app was already playing music when it would start.

## Not built yet

- Multiple concurrent products per lane (a second app, two games in parallel).
- Rival companies as full simulations, plus acquisitions you make yourself.
- Hand-drawn art. (Everything is drawn in code: the isometric office with each office tier, its perks and its people, the staff portraits, and box art painted for every game in layers: a scene of its own for each of the 37 topics, a rim-lit hero for the genre, atmosphere, a title logo with the sequel number picked out, the platform band, the rating and the studio.)
- The studio update, the running clock, the removal of energy and the 0.5 gameplay update in the Swift (macOS and terminal) version.
- Steam-style achievements per ending variant.
- Proper tuning passes with real playtesters; the current numbers come from autopilot runs.
