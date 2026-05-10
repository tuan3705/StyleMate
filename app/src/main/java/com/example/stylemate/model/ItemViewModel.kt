package com.example.stylemate.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemViewModel(private val itemDao: ItemDao) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(Categories.ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory

    val items: StateFlow<List<Item>> = _selectedCategory.flatMapLatest { category ->
        if (category == Categories.ALL) {
            itemDao.getAllItems()
        } else {
            itemDao.getItemsByCategory(category)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun getItemCount(category: String): Flow<Int> {
        return if (category == Categories.ALL) {
            itemDao.getItemCount()
        } else {
            itemDao.getItemCountByCategory(category)
        }
    }

    fun addItem(item: Item) {
        viewModelScope.launch {
            itemDao.insertItem(item)
        }
    }
}

class ItemViewModelFactory(private val itemDao: ItemDao) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
