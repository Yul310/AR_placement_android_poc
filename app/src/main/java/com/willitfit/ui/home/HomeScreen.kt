package com.willitfit.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willitfit.data.model.MeasurementMode
import com.willitfit.data.model.Product
import com.willitfit.ui.home.components.ModeSelectionSheet
import com.willitfit.ui.home.components.ProductCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMeasurement: (Product, MeasurementMode) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showModeSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WillItFit",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                products.isEmpty() -> {
                    Text(
                        text = "No products available",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    ProductList(
                        products = products,
                        onProductClick = { product ->
                            selectedProduct = product
                            showModeSheet = true
                        }
                    )
                }
            }
        }

        if (showModeSheet && selectedProduct != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showModeSheet = false
                    selectedProduct = null
                },
                sheetState = sheetState
            ) {
                ModeSelectionSheet(
                    product = selectedProduct!!,
                    onModeSelected = { mode ->
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            selectedProduct?.let { product ->
                                onNavigateToMeasurement(product, mode)
                            }
                            showModeSheet = false
                            selectedProduct = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProductList(
    products: List<Product>,
    onProductClick: (Product) -> Unit
) {
    val groupedProducts = products.groupBy { it.category }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedProducts.forEach { (category, categoryProducts) ->
            item {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(categoryProducts) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
