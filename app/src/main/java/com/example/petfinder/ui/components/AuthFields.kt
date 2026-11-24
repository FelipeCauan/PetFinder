package com.example.petfinder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

/**
 * Campo de texto padronizado no estilo dos mockups do PetFinder:
 * rótulo externo acima, dica opcional à direita e caixa preenchida arredondada sem borda.
 */
@Composable
fun PetFinderTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailingHint: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    errorText: String? = null,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (trailingHint != null) {
                Text(
                    text = trailingHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = true,
            isError = errorText != null,
            enabled = enabled,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Botão primário em formato pílula usado nas telas de Login e Cadastro.
 */
@Composable
fun PetFinderPrimaryButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Máscara visual 000.000.000-00 para CPF armazenado apenas com dígitos.
 */
class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(11)
        val formatted = buildString {
            for (i in trimmed.indices) {
                append(trimmed[i])
                if (i == 2 && trimmed.length > 3) append('.')
                if (i == 5 && trimmed.length > 6) append('.')
                if (i == 8 && trimmed.length > 9) append('-')
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var extra = 0
                if (trimmed.length > 3 && offset > 3) extra++
                if (trimmed.length > 6 && offset > 6) extra++
                if (trimmed.length > 9 && offset > 9) extra++
                return (offset + extra).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                var sub = 0
                if (trimmed.length > 3 && offset > 3) sub++
                if (trimmed.length > 6 && offset > 7) sub++
                if (trimmed.length > 9 && offset > 11) sub++
                return (offset - sub).coerceIn(0, trimmed.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

/**
 * Máscara visual 00000-000 para CEP armazenado apenas com dígitos.
 */
class CepVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(8)
        val formatted = buildString {
            for (i in trimmed.indices) {
                append(trimmed[i])
                if (i == 4 && trimmed.length > 5) append('-')
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val extra = if (trimmed.length > 5 && offset > 5) 1 else 0
                return (offset + extra).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val sub = if (trimmed.length > 5 && offset > 5) 1 else 0
                return (offset - sub).coerceIn(0, trimmed.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
