package com.example.predlozak_1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.predlozak_1.ui.theme.Predlozak_1Theme
import kotlinx.coroutines.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.KeyboardType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Predlozak_1Theme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    UserPreview(191,100,

                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun UserPreview(heightCm: Int, initialWeightKg: Int, modifier: Modifier = Modifier) {
    // Izračun BMI-a
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var rezultat by remember { mutableStateOf("") }
    var newWeightText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var newBmi by remember { mutableStateOf<Float?>(null) }


    val heightMeters = heightCm / 100f
    val initialBmi = initialWeightKg / (heightMeters * heightMeters)
    val idealBmi = 21.7f
    var progress by remember { mutableStateOf(0f) }




    // Određivanje poruke na temelju BMI-a
    val bmiStatus = when {
        initialBmi < 18.5 -> "Prenizak BMI"
        initialBmi in 18.5..24.9 -> "Idealan BMI"
        else -> "Previsok BMI"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {

        Image(
            painter = painterResource(id = R.drawable.fitness),
            contentDescription = "Pozadinska slika",
            contentScale = ContentScale.Crop,
            alpha = 0.1f,
            modifier = Modifier.fillMaxSize()
        )


        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Profilna slika
            Image(
                painter = painterResource(id = R.drawable.profile_pic),
                contentDescription = "Profilna slika",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))


            Column {
                Text(
                    text = "Pozdrav, Miljenko",
                    fontSize = 18.sp
                )
                Text(
                    text = bmiStatus,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            delay(2000L) // simulacija "računanja"
                            val idealBmi = 21.7
                            val razlika = (initialBmi - idealBmi).let { kotlin.math.abs(it) }
                            rezultat = "Udaljeni ste %.1f od idealnog BMI-a.".format(razlika)
                            isLoading = false
                        }
                    }) {
                        Text("Izračunaj razliku od idealnog BMI")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(text = rezultat)

                        // Unos nove težine
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = newWeightText,
                            onValueChange = { input ->
                                newWeightText = input.filter { it.isDigit() || it == '.' }
                            },
                            label = { Text("Unesite novu težinu (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Gumb za izračun napretka
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val enteredWeight = newWeightText.toFloatOrNull()
                                if (enteredWeight != null && heightMeters > 0) {
                                    scope.launch {
                                        isProcessing = true
                                        delay(1000L)

                                        val calculatedBmi = enteredWeight / (heightMeters * heightMeters)
                                        newBmi = calculatedBmi

                                        progress = when {
                                            calculatedBmi >= initialBmi -> 0f
                                            calculatedBmi <= idealBmi -> 1f
                                            else -> {
                                                val ukupnaRazlika = initialBmi - idealBmi
                                                val postignutaRazlika = initialBmi - calculatedBmi
                                                postignutaRazlika / ukupnaRazlika
                                            }
                                        }

                                        isProcessing = false
                                    }
                                }
                            },
                            enabled = !isProcessing
                        ) {
                            Text("Izračunaj napredak")
                        }

                        // Prikaz progres bara
                        Spacer(modifier = Modifier.height(12.dp))
                        if (newBmi != null) {
                            Text("Novi BMI: %.1f – Napredak: %.0f%%".format(newBmi, progress * 100))
                            LinearProgressIndicator(
                                progress = progress.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }

                        if (isProcessing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator()
                        }
                    }
                    }
                }
            }
        }
    }


