package org.alexmagter.QuickYTD

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
    private val fieldBackgroundColor: Color = Color(64, 64, 64, 50)

    @Composable
    fun textFieldColors() = TextFieldDefaults.colors(
        focusedContainerColor = fieldBackgroundColor,
        unfocusedContainerColor = fieldBackgroundColor,
        disabledContainerColor = fieldBackgroundColor,

        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,

        cursorColor = Color.White,

        focusedIndicatorColor = Color.LightGray,
        unfocusedIndicatorColor = Color.LightGray,
        disabledIndicatorColor = Color.Red,

        focusedPlaceholderColor = Color.Gray,
        unfocusedPlaceholderColor = Color.LightGray,
        disabledPlaceholderColor = Color.DarkGray,
        errorPlaceholderColor = Color.Red,

        focusedLabelColor = Color.Gray,
        unfocusedLabelColor = Color.LightGray,
        disabledLabelColor = Color.DarkGray,
        errorLabelColor = Color.Red
    )

    @Composable
    fun SearcbButtonColors(isEnabled: Boolean) =
        ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.DarkGray
        )

    @Composable
    fun ButtonColors(isEnabled: Boolean) =
        ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.DarkGray
        )

    @Composable
    fun CancelButtonColors(isEnabled: Boolean) =
        ButtonDefaults.buttonColors(
            containerColor = Color.Red,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.DarkGray
        )


}