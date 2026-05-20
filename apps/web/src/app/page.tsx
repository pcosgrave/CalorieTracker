import type { FoodProduct } from "@calorie-tracker/shared";
import styles from "./page.module.css";

const sampleFoods: FoodProduct[] = [
  {
    productId: "sample-1",
    ownerUserId: "local",
    visibility: "private",
    barcode: "012345678905",
    name: "Greek yogurt",
    brand: "Sample Dairy",
    serving: {
      label: "1 cup",
      quantity: 1,
      unit: "cup",
      grams: 227,
    },
    nutrients: {
      calories: 140,
      proteinGrams: 20,
      carbohydrateGrams: 8,
      fatGrams: 3,
    },
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

export default function Home() {
  const totals = sampleFoods.reduce(
    (acc, food) => ({
      calories: acc.calories + food.nutrients.calories,
      protein: acc.protein + food.nutrients.proteinGrams,
      carbs: acc.carbs + food.nutrients.carbohydrateGrams,
      fat: acc.fat + food.nutrients.fatGrams,
    }),
    { calories: 0, protein: 0, carbs: 0, fat: 0 },
  );

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div className={styles.brand}>
            <h1>CalorieTracker</h1>
            <p>Manual labels, private barcode shortcuts, cloud sync when signed in.</p>
          </div>
          <button type="button">Sign in with Google</button>
        </header>

        <section className={styles.grid}>
          <div className={styles.panel}>
            <h2>Today</h2>
            <div className={styles.daySummary}>
              <div className={styles.metric}>
                <strong>{totals.calories}</strong>
                <span>Calories</span>
              </div>
              <div className={styles.metric}>
                <strong>{totals.protein}g</strong>
                <span>Protein</span>
              </div>
              <div className={styles.metric}>
                <strong>{totals.carbs}g</strong>
                <span>Carbs</span>
              </div>
              <div className={styles.metric}>
                <strong>{totals.fat}g</strong>
                <span>Fat</span>
              </div>
            </div>

            <div className={styles.list}>
              {sampleFoods.map((food) => (
                <article className={styles.item} key={food.productId}>
                  <div className={styles.itemTitle}>
                    <strong>{food.name}</strong>
                    <span>
                      {food.brand} · {food.serving.label} · {food.barcode}
                    </span>
                  </div>
                  <strong>{food.nutrients.calories} cal</strong>
                </article>
              ))}
            </div>
          </div>

          <div className={styles.panel}>
            <h2>Add Food From Label</h2>
            <form className={styles.form}>
              <label>
                Barcode
                <input inputMode="numeric" name="barcode" placeholder="Scan or type barcode" />
              </label>
              <div className={styles.twoColumn}>
                <label>
                  Product name
                  <input name="name" placeholder="Greek yogurt" />
                </label>
                <label>
                  Brand
                  <input name="brand" placeholder="Sample Dairy" />
                </label>
              </div>
              <div className={styles.twoColumn}>
                <label>
                  Serving label
                  <input name="servingLabel" placeholder="1 cup" />
                </label>
                <label>
                  Serving grams
                  <input inputMode="decimal" name="grams" placeholder="227" />
                </label>
              </div>
              <div className={styles.fourColumn}>
                <label>
                  Calories
                  <input inputMode="decimal" name="calories" placeholder="140" />
                </label>
                <label>
                  Protein
                  <input inputMode="decimal" name="protein" placeholder="20" />
                </label>
                <label>
                  Carbs
                  <input inputMode="decimal" name="carbs" placeholder="8" />
                </label>
                <label>
                  Fat
                  <input inputMode="decimal" name="fat" placeholder="3" />
                </label>
              </div>
              <div className={styles.actions}>
                <button className="secondary" type="button">
                  Clear
                </button>
                <button type="button">Save Food</button>
              </div>
            </form>
          </div>
        </section>
      </div>
    </main>
  );
}
