package com.theplace.receiptscanner.ui

import com.theplace.receiptscanner.data.ReceiptCategory
import com.theplace.receiptscanner.resources.Res
import com.theplace.receiptscanner.resources.category_fuel
import com.theplace.receiptscanner.resources.category_groceries
import com.theplace.receiptscanner.resources.category_health
import com.theplace.receiptscanner.resources.category_other
import com.theplace.receiptscanner.resources.category_restaurant
import com.theplace.receiptscanner.resources.category_shopping
import org.jetbrains.compose.resources.StringResource

/** Mapping enum → ressource string, factorisé pour les chips et le picker du détail. */
fun ReceiptCategory.labelRes(): StringResource = when (this) {
    ReceiptCategory.Groceries -> Res.string.category_groceries
    ReceiptCategory.Restaurant -> Res.string.category_restaurant
    ReceiptCategory.Fuel -> Res.string.category_fuel
    ReceiptCategory.Health -> Res.string.category_health
    ReceiptCategory.Shopping -> Res.string.category_shopping
    ReceiptCategory.Other -> Res.string.category_other
}
