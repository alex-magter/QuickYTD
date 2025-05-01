package org.example.project

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object DarkTheme {
    val backgroundColor: Color = Color(25, 25, 25, 255)
    val secondaryBackgroundColor: Color = Color(20, 20, 20, 255)
    val dropdownShape = RoundedCornerShape(15.dp)

    @Composable
    fun textFieldColors() = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = Color.White,
        focusedIndicatorColor = Color.Gray,
        unfocusedIndicatorColor = Color.Gray,
        disabledIndicatorColor = Color.Red
    )

    @Composable
    fun ButtonColors(isEnabled: Boolean) =
        ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.DarkGray
        )





}