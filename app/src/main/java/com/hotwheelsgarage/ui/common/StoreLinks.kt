package com.hotwheelsgarage.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hotwheelsgarage.data.Http

/** Quick-commerce and shopping searches. Links open the store's app when it's installed. */
private val stores = listOf(
    "Blinkit" to "https://blinkit.com/s/?q=",
    "Zepto" to "https://www.zepto.com/search?query=",
    "Flipkart Minutes" to "https://www.flipkart.com/search?marketplace=HYPERLOCAL&q=",
    "Instamart" to "https://www.swiggy.com/instamart/search?query=",
    "Amazon" to "https://www.amazon.in/s?k=",
)

/** One-tap "is it in stock?" row: opens each store already searching for this car. */
@Composable
fun StoreLinks(carName: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val query = Http.encode("hot wheels $carName")
    Column(modifier) {
        Text("Check stores", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stores.forEach { (name, url) ->
                AssistChip(
                    onClick = { runCatching { uriHandler.openUri(url + query) } },
                    label = { Text(name) },
                    leadingIcon = { Icon(Icons.Filled.ShoppingCart, null) },
                )
            }
        }
    }
}
