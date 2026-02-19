package com.willitfit.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willitfit.data.model.Product
import com.willitfit.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProductRepository(application)

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _products.value = repository.loadProducts()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load products: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadProducts()
    }

    fun getProductsByCategory(): Map<String, List<Product>> {
        return _products.value.groupBy { it.category }
    }
}
