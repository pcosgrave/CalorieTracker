# BiteWise — Future Features & Product Direction

This document captures future product and architecture ideas for BiteWise.

These are not all immediate implementation requirements. They describe the direction the product may evolve toward and should be considered when designing current data models and architecture.

---

# 1. Product Direction

BiteWise starts as a calorie and nutrition tracker, but the longer-term goal is broader:

> Help users decide what to eat, understand how much to eat, use the food they already have, reduce waste, and make food management easier for the household.

The app should avoid becoming a high-maintenance inventory system.

The core principle is:

**BiteWise should learn from normal user behavior rather than requiring users to constantly maintain data.**

For example:

- Logging food can update knowledge about what is being consumed.
- Grocery scanning can update what is probably available.
- Leftovers can become available food for future meals.
- Expiring food can influence meal recommendations.
- Previous meals can influence future suggestions.
- Household activity can update shared food state.

AI should generally assist with inference and recommendations rather than requiring perfect information.

---

# 2. Core Food Model

BiteWise should distinguish between several concepts that may currently overlap.

## Ingredient / Food

Represents something nutritionally known.

Examples:

- Chicken breast
- Gala apple
- Cheddar cheese
- Cheerios
- Bell pepper

Contains information such as:

- Calories
- Macronutrients
- Serving units
- Weight conversions
- Nutrition source
- Barcode/UPC where applicable

This answers:

> "What is this food?"

It does NOT necessarily mean the user currently owns it.

---

# 3. Recipe

A Recipe describes how multiple ingredients are combined to create a prepared food.

Example:

## Lasagna

Ingredients:

- Ground beef
- Pasta
- Tomato sauce
- Ricotta
- Mozzarella

A recipe contains:

- Ingredient quantities
- Instructions
- Total nutritional information
- Serving information
- Optional image

Recipes primarily answer:

> "How do I make this?"

---

# 4. Meal

Introduce a **Meal** concept above Ingredients and Recipes.

A Meal represents the collection of food actually being eaten together.

Example:

## Lasagna Dinner

Contains:

- 1 serving Lasagna Recipe
- Garlic bread
- Caesar salad

Another example:

## Chicken Pasta Dinner

Contains:

- Chicken breast
- Pasta
- Pasta sauce
- Parmesan
- Garlic bread

A Meal can therefore contain:

- Recipes
- Ingredients
- Other prepared food components

Conceptually:

Ingredient → Recipe → Meal

But a Meal does not require a Recipe.

For example:

## Breakfast

- 2 eggs
- Toast
- Banana
- Coffee

This can be a Meal without creating a recipe.

The distinction is:

> **Recipes describe making food. Meals describe eating food.**

---

# 5. Meal UI

Opening a Meal should provide a simple overview of everything required.

Example:

## Chicken Pasta Dinner

**Target: ~650 calories**

Required:

- Chicken breast — 150 g
- Pasta — 85 g dry
- Sauce — 120 g
- Parmesan — 15 g

The user should be able to:

- Prepare the meal
- Adjust quantities
- See resulting calories/macros
- Log the meal
- Allocate portions
- Save changes as the usual version of the meal

The interface should avoid forcing the user to navigate separately through every ingredient unless they want to.

---

# 6. Individual vs Shared Meal Preparation

BiteWise should support two preparation styles.

## Individually Assembled

Example: stir-fry where each household member prepares their own portion.

Philip's stir-fry might contain:

- 100 g ramen
- 150 g chicken
- 75 g cabbage
- 50 g peppers
- 30 g onion

Another household member's version can have different quantities.

Each person effectively has their own Meal instance.

## Communally Prepared

Example:

A large pot of chicken pasta is prepared for the household.

The entire prepared meal might contain 2,400 calories.

After cooking, BiteWise can provide a simple allocation UI:

**Who is eating this?**

- Philip — 40%
- Household member — 40%
- Leftovers — 20%

The percentages must total 100%.

Alternatively, allocation could be based on weight:

- Philip — 600 g
- Household member — 550 g
- Leftovers — 300 g

BiteWise calculates calories and macros automatically.

---

# 7. Automatic Leftover Creation

Meal allocation should integrate directly with leftovers.

Example:

Chicken Pasta

- Philip: 40%
- Household member: 40%
- Leftover: 20%

Selecting 20% leftover automatically creates:

**Chicken Pasta Leftover**

Containing 20% of the original meal's:

- Calories
- Macronutrients
- Ingredient quantities

The user should not have to manually recreate the leftover.

---

# 8. Adaptive Portion Recommendations

BiteWise should eventually recommend **how much of a meal the user should eat**, rather than simply showing remaining calories.

The recommendation can consider:

- Daily calorie target
- Calories already consumed
- Typical breakfast calories
- Typical lunch calories
- Typical dinner calories
- Snacks
- Remaining expected meals
- User goals
- Previous portions of this meal

Example:

> Dinner target: ~680 calories

Instead of:

> You have 680 calories remaining.

BiteWise could say:

> For tonight's chicken pasta, approximately 32% of the prepared meal would fit your target.

Or:

> Aim for approximately 520 g.

The recommendation should remain guidance rather than forcing a portion.

---

# 9. Household / Family Accounts

Support linking multiple BiteWise accounts into a household.

A household could contain:

- Adults with BiteWise accounts
- Potentially household members without accounts
- Shared food
- Shared leftovers
- Shared pantry information

Household data should be stored/synchronized through the backend.

Potential permissions:

- Household owner/admin
- Adult member
- Child/dependent

Avoid overcomplicating permissions initially.

---

# 10. Shared Leftovers

Leftovers can belong to the household rather than only one user.

Example:

Philip creates:

**Chicken Pasta Leftover — 650 calories**

Another household member should be able to consume it.

When consumed:

- It disappears/reduces from the household leftover pool.
- Nutrition is logged against the person who consumed it.

This requires server synchronization to prevent two people from consuming the same leftover simultaneously.

---

# 11. Leftover Expiry

When creating a leftover, allow an optional expiry date.

BiteWise can suggest a reasonable default where appropriate, but the user should be able to modify it.

Example:

**Chicken Pasta**

Created: Monday

Suggested use by: Thursday

Notifications could include:

> Chicken pasta should probably be eaten tomorrow.

Expiry should later influence meal recommendations.

---

# 12. Pantry

Introduce the concept of a household Pantry.

The pantry should NOT attempt to be a perfectly accurate inventory system.

It represents:

> "What food does BiteWise believe is probably available?"

This distinction is important because household members may consume food without logging it.

---

# 13. Food vs Package vs Pantry State

Keep these concepts separate.

## Food

Nutritional definition.

Example:

Gala Apple

## Package

Something purchased.

Example:

3 lb bag of Gala apples

## Pantry State

BiteWise's current understanding of what remains.

Example:

Gala apples  
Approximately 5 remaining

The number does not need to be exact.

Other useful states:

- Available
- Probably available
- Running low
- Out
- Unknown

This prevents the pantry from pretending to have precision it cannot realistically maintain.

---

# 14. AI-Assisted Pantry Understanding

Local AI can help interpret purchased products.

For example, scanning or photographing:

> 3 lb bag of Gala apples

could result in:

**Detected**

Gala Apples  
3 lb package  
Approximately 8 apples

BiteWise could ask:

> Add approximately 8 apples to your pantry?

The approximation should remain explicit.

AI could similarly identify:

- Produce
- Packaged foods
- Multipacks
- Common household food products

---

# 15. Grocery Scanning

Users could scan groceries while shopping or while putting groceries away.

Scanning a product can:

1. Identify the food.
2. Add it to the user's food database if necessary.
3. Add the purchased package to the pantry.
4. Update approximate pantry availability.

Barcode scanning should be preferred where reliable.

Camera/AI recognition can handle items without useful barcodes, particularly produce.

---

# 16. Grocery List Integration

Investigate integration with external grocery/shopping-list providers.

Potential examples include household assistant ecosystems and grocery-list services.

Desired flow:

1. Grocery list appears in BiteWise.
2. User shops.
3. Items are checked off.
4. Purchased items can optionally enter the BiteWise pantry.
5. Pantry information becomes available for meal recommendations.

Do not make the core pantry architecture dependent on any specific external provider.

---

# 17. Smart Grocery Suggestions

BiteWise can eventually understand relationships between commonly purchased ingredients.

Example:

User adds taco meat.

BiteWise might recognize that the household commonly makes tacos and ask:

> You usually make tacos with this. Do you also need taco seasoning, tortillas, lettuce, or cheese?

Recommendations should consider:

- Previous meals
- Recipes
- Pantry contents
- Grocery history

Avoid generic recommendations when BiteWise already knows the household's usual version.

---

# 18. "What Should We Eat?" Recommendations

A major future feature should answer:

> What should I make for dinner?

Recommendations can consider:

- Pantry availability
- Leftovers
- Food nearing expiry
- Household preferences
- Previous meals
- Recent meal frequency
- Calorie targets
- Time of day
- Typical meals
- Recipes already saved

Example:

## Dinner Ideas

**Chicken Stir-Fry**

You already have most ingredients.

Uses:
- Chicken
- Pepper that should be used soon
- Onion
- Snow peas

**Leftover Lasagna**

Ready immediately and should be eaten today.

**Tacos**

You have the meat and seasoning but may need tortillas.

The goal is to provide a few useful options rather than dozens of generic recipes.

---

# 19. Expiring Food → Meal Suggestions

Perishable pantry items can influence recommendations.

Example:

BiteWise knows:

- Bell pepper is approaching its expected expiry.
- Chicken is available.
- The household frequently makes stir-fry.

A notification might say:

> You have a pepper that should probably be used soon. Want some dinner ideas that use it?

Tapping the notification opens meal suggestions with that ingredient prioritized.

---

# 20. Local Notifications

Personal meal reminders should primarily run on-device.

Settings could include:

**Meal Suggestions**

- Breakfast suggestions
- Lunch suggestions
- Dinner suggestions

Initially, users can configure approximate times.

Example:

Dinner reminder: 4:30 PM

Eventually BiteWise could learn typical meal times and suggest schedule adjustments.

The notification itself does not need to generate the meal recommendation.

Example:

> Not sure what's for dinner? I have a few ideas.

Tapping it opens the recommendation screen, where the local model generates/ranks suggestions using current context.

This avoids unnecessary cloud AI calls.

---

# 21. AWS Responsibilities

AWS should primarily handle data that must be shared or synchronized.

Examples:

- Authentication
- Household membership
- Shared leftovers
- Shared pantry state
- Cross-device synchronization
- Household permissions
- Shared data conflict resolution
- Push notification delivery where server-side events are required

AWS should NOT automatically become responsible for every AI operation.

Prefer local/on-device processing where practical.

---

# 22. Scheduled Backend Jobs

Some shared functionality benefits from scheduled AWS jobs.

Example:

Once each morning:

EventBridge Scheduler
        ↓
Lambda
        ↓
Query expiring household food
        ↓
Determine whether notification is useful
        ↓
Send push notification

This infrastructure should be managed through Terraform.

Start with simple periodic batch processing rather than creating individual scheduled AWS jobs for every pantry item.

---

# 23. Local AI Responsibilities

Prefer on-device AI for personalized operations that do not require shared server state.

Potential responsibilities:

- Meal ranking
- "What should I eat?" suggestions
- Recognizing meals from photos
- Understanding grocery/package descriptions
- Normalizing food names
- Suggesting relationships between foods
- Learning common household meals
- Interpreting natural-language food input

Advantages:

- Lower cloud cost
- Better privacy
- Fast interaction
- Offline capability

Cloud AI can remain an optional fallback where local models cannot reliably perform a task.

---

# 24. Visual Meal Recognition

Photo logging should eventually do more than estimate nutrition.

BiteWise can learn what the user's own meals look like.

Example:

The user repeatedly logs their usual tacos.

Later they photograph dinner.

BiteWise might suggest:

> This looks like your usual taco dinner.

Actions:

- Log usual portion
- Adjust portion
- Not this meal

The goal is not necessarily perfect calorie estimation from pixels.

The photo is another signal combined with:

- Previous meals
- Time of day
- Recent foods
- Saved recipes
- Household patterns

---

# 25. Meal History and Reuse

When several foods are logged together for dinner, BiteWise can recognize them as a Meal.

Example diary:

## Dinner — 684 calories

Chicken  
Roasted potatoes  
Broccoli  
Gravy

Actions:

- Log again
- Save as Meal
- Adjust portions

This means users do not necessarily need to explicitly create every Meal beforehand.

BiteWise can learn them organically from normal logging behavior.

---

# 26. Food Waste Reduction

BiteWise should provide positive feedback around using food already available.

Potential future metrics:

- Leftovers consumed
- Expiring food used
- Food potentially saved from waste
- Approximate grocery savings

Avoid making this feel punitive.

The experience should feel like:

> BiteWise helped me use what I already had.

rather than:

> BiteWise is judging what I threw away.

---

# 27. Core UX Principle

All of these systems can become extremely complicated.

The complexity should live primarily in the data model and inference system — **not in the user interface.**

The user should still be able to perform common actions extremely quickly:

- Log food
- Take a photo
- Scan food
- Ask for dinner ideas
- Log a known meal
- Split a prepared meal
- Create a leftover

The user should not need to maintain a perfect digital representation of their kitchen.

BiteWise should tolerate uncertainty.

---

# 28. Confidence / Approximation

Many pantry and AI-derived values are inherently uncertain.

The data model should eventually support the concept of confidence.

Examples:

**Known**

> 2 cans of tomato sauce

**Estimated**

> ~5 apples remaining

**Probably available**

> Taco seasoning

**Unknown quantity**

> Rice

This is preferable to storing incorrect precision.

---

# 29. Suggested Implementation Order

Do not attempt to build the entire vision at once.

### Near Term

1. Introduce Meal data model.
2. Allow Meals to contain Recipes + Ingredients.
3. Build simple Meal detail UI.
4. Log an entire Meal.
5. Add shared-meal percentage/weight allocation.
6. Automatically create leftovers from unallocated portions.
7. Add leftover expiry dates.
8. Add local meal reminder settings.

### Next

9. Household account model.
10. Shared leftovers.
11. Daily AWS expiry check.
12. Push notifications.
13. Basic pantry model.
14. Grocery scanning → pantry.
15. Expiring-food meal recommendations.

### Later

16. AI dinner recommendations.
17. Learn frequently eaten Meals.
18. Visual meal recognition.
19. Approximate pantry quantities.
20. Smart grocery recommendations.
21. External grocery-list integrations.
22. Adaptive portion recommendations.
23. Food-waste/savings insights.

---

# 30. Architectural Principle

When implementing current features, avoid architecture that makes these future concepts unnecessarily difficult.

In particular:

- Do not treat Ingredient, Recipe, Meal, Leftover and PantryItem as interchangeable concepts.
- Do not assume all food belongs to exactly one user.
- Do not assume pantry quantities are exact.
- Do not require cloud AI for functionality that could run locally.
- Do not require server connectivity for basic calorie logging.
- Keep household/shared state separate from personal state.
- Allow meals to reference both recipes and ingredients.
- Design leftovers so they can originate from meals or recipes.
- Preserve enough nutritional information to recalculate portions later.

The immediate product should remain simple while leaving room for these concepts.