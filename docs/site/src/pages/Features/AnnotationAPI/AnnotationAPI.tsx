import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const annotationExample = `// 1. Define your graph with annotations
@Graph("shop")
sealed class ShopDestination : Destination

// 2. Add destinations with routes
@Route("shop/products")
data object ProductList : ShopDestination()

// 3. Add typed destinations with arguments
@Serializable
data class ProductData(val productId: String, val mode: String = "view")

@Route("shop/product/detail")
@Argument(ProductData::class)
data class ProductDetail(
    val productId: String,
    val mode: String = "view"
) : ShopDestination(), TypedDestination<ProductData> {
    override val data = ProductData(productId, mode)
}

// 4. Define content with @Content annotation
@Content(ProductList::class)
@Composable
fun ProductListContent(navigator: Navigator) {
    ProductListScreen(
        onProductClick = { id ->
            // Type-safe navigation with generated extension
            navigator.navigateToProductDetail(
                productId = id,
                mode = "view"
            )
        }
    )
}

@Content(ProductDetail::class)
@Composable
fun ProductDetailContent(data: ProductData, navigator: Navigator) {
    ProductDetailScreen(
        productId = data.productId,
        mode = data.mode,
        onBack = { navigator.navigateBack() }
    )
}

// 5. Use generated graph builder
val shopGraph = buildShopDestinationGraph()

// That's it! KSP generates:
// - Route registration (ShopDestinationRouteInitializer)
// - Graph builder (buildShopDestinationGraph())
// - Typed navigation extensions (navigateToProductDetail())`

export default function AnnotationAPI() {
  return (
    <article className={styles.features}>
      <h1>Annotation-Based API with Code Generation</h1>
      <p className={styles.intro}>
        The modern, recommended approach to building navigation in Quo Vadis. 
        Use simple annotations on your destinations and let KSP generate all the boilerplate 
        code automatically. This approach combines compile-time safety with minimal code.
      </p>

      <div className={styles.highlights}>
        <ul>
          <li><strong>Zero Boilerplate:</strong> No manual graph builders, route registration, or destination factories</li>
          <li><strong>Type-Safe Arguments:</strong> Automatic serialization/deserialization with kotlinx.serialization</li>
          <li><strong>IDE Support:</strong> Full autocompletion and navigation for generated code</li>
          <li><strong>Compile-Time Verification:</strong> Catch errors before runtime</li>
        </ul>
      </div>

      <section>
        <h2 id="four-annotations">The Four Annotations</h2>
        <div className={styles.annotationGrid}>
          <div className={styles.annotationCard}>
            <h4>@Graph</h4>
            <p>Marks a sealed class as a navigation graph. Generates graph builder functions.</p>
          </div>
          <div className={styles.annotationCard}>
            <h4>@Route</h4>
            <p>Specifies the route path. Automatically registers routes with the system.</p>
          </div>
          <div className={styles.annotationCard}>
            <h4>@Argument</h4>
            <p>Defines typed, serializable arguments. Generates typed destination extensions.</p>
          </div>
          <div className={styles.annotationCard}>
            <h4>@Content</h4>
            <p>Connects Composable functions to destinations. Wired automatically in graph.</p>
          </div>
        </div>
      </section>

      <section>
        <h2 id="complete-example">Complete Example</h2>
        <CodeBlock code={annotationExample} language="kotlin" />
      </section>

      <section>
        <h2 id="what-gets-generated">What Gets Generated</h2>
        <ul>
          <li><strong>Route Initializers:</strong> Automatic route registration objects</li>
          <li><strong>Graph Builders:</strong> <code>build&#123;GraphName&#125;Graph()</code> functions</li>
          <li><strong>Typed Extensions:</strong> <code>navigateTo&#123;DestinationName&#125;()</code> functions</li>
          <li><strong>Serialization Code:</strong> Argument encoding/decoding logic</li>
        </ul>

        <p style={{ marginTop: '1.5rem' }}>
          <strong>Benefits over manual DSL:</strong> Write 50-70% less code, no manual route registration needed, 
          automatic argument serialization, generated code is type-safe and tested, easier to maintain and refactor.
        </p>

        <div className={styles.note}>
          <strong>Note:</strong> The manual DSL approach is still fully supported for advanced use cases. 
          See <a href="/features/type-safe">Type-Safe Navigation</a> for the manual approach.
        </div>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/getting-started">Get started</a> with the quick start guide</li>
          <li><a href="/features/type-safe">Type-Safe Navigation</a> - Learn about the manual DSL approach</li>
          <li><a href="/demo">See the demo</a> to explore features in action</li>
        </ul>
      </section>
    </article>
  )
}
