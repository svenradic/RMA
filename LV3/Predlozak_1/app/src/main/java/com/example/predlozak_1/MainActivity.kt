package com.example.predlozak_1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.predlozak_1.ui.theme.Predlozak_1Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.floatPreferencesKey
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.pow

val Context.dataStore by preferencesDataStore(name = "user_prefs")
val REZULTAT_KEY = stringPreferencesKey("rezultat_bmi")
suspend fun spremiRezultat(context: Context, tekst: String) {
    context.dataStore.edit { preferences ->
        preferences[REZULTAT_KEY] = tekst }
}
fun dohvatiRezultat(context: Context): Flow<String> {
    return context.dataStore.data.map { preferences ->
        preferences[REZULTAT_KEY] ?: ""
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Predlozak_1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UserPreview(
                        heightCm = 191,
                        weightKg = 100,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun UserPreview(heightCm: Int, weightKg: Int, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var rezultat by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var newWeightInput by remember { mutableStateOf("") }
    var bmiNapredakTekst by remember { mutableStateOf("") }
    val db = FirebaseFirestore.getInstance()
    val heightMeters = heightCm / 100f
    val bmi = weightKg / (heightMeters * heightMeters)
    val idealBmi = 21.7f
    val context = LocalContext.current

    var minTezinaText by remember { mutableStateOf("") }
    var maxTezinaText by remember { mutableStateOf("") }

    fun dohvatiMinMaxBMI(
        db: FirebaseFirestore,
        onResult: (String, String) -> Unit
    ) {
        db.collection("BMI")
            .get()
            .addOnSuccessListener { documents ->
                val podaci = documents.mapNotNull { doc ->
                    val visina = doc.getDouble("Visina")?.toFloat()
                    val tezina = doc.getDouble("Tezina")?.toFloat()
                    if (visina != null && tezina != null && visina > 0) {
                        Pair(tezina, visina)
                    } else null
                }

                if (podaci.isNotEmpty()) {
                    val min = podaci.minBy { it.first }
                    val max = podaci.maxBy { it.first }

                    val bmiMin = min.first / ((min.second / 100f).pow(2))
                    val bmiMax = max.first / ((max.second / 100f).pow(2))

                    val minText = "Najmanja težina: %.1f kg (BMI: %.1f)".format(min.first, bmiMin)
                    val maxText = "Najveća težina: %.1f kg (BMI: %.1f)".format(max.first, bmiMax)

                    onResult(minText, maxText)
                } else {
                    onResult("Nema podataka", "")
                }
            }
            .addOnFailureListener {
                onResult("Greška pri dohvaćanju", "")
            }
    }

    LaunchedEffect(true) {
        dohvatiMinMaxBMI(db) { min, max ->
            minTezinaText = min
            maxTezinaText = max
        }
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

        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_pic),
                    contentDescription = "Profilna slika",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Pozdrav, Miljenko", fontSize = 18.sp)
                    Text(text = when {
                        bmi < 18.5 -> "Prenizak BMI"
                        bmi in 18.5..24.9 -> "Idealan BMI"
                        else -> "Previsok BMI"
                    }, fontSize = 14.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                scope.launch {
                    isLoading = true
                    delay(2000L)
                    val razlika = kotlin.math.abs(bmi - idealBmi)
                    val tekst = "Udaljeni ste %.1f od idealnog BMI-a.".format(razlika)
                    rezultat = tekst
                    spremiRezultat(context, tekst)
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = newWeightInput,
                onValueChange = { newWeightInput = it },
                label = { Text("Unesite novu težinu (kg)") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                scope.launch {
                    isLoading = true
                    delay(1000L)
                    val novaTezina = newWeightInput.toFloatOrNull()
                    if (novaTezina != null && novaTezina > 0f) {
                        val podatak = hashMapOf(
                            "Visina" to heightCm,
                            "Tezina" to novaTezina,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                        db.collection("BMI")
                            .add(podatak)
                            .addOnSuccessListener {
                                bmiNapredakTekst = "Podatak spremljen u Firebase!"
                            }
                            .addOnFailureListener {
                                bmiNapredakTekst = "Greška pri spremanju."
                            }
                        val noviBmi = novaTezina / (heightMeters * heightMeters)
                        val ukupnoZaSpustiti = (bmi - idealBmi).coerceAtLeast(0f)
                        val vecSpusteno = (bmi - noviBmi).coerceAtLeast(0f)
                        progress = if (ukupnoZaSpustiti > 0f) {
                            (vecSpusteno / ukupnoZaSpustiti).coerceIn(0f, 1f)
                        } else 1f
                        bmiNapredakTekst = "Novi BMI: %.1f – Napredak: %.0f%%".format(noviBmi, progress * 100)
                        dohvatiMinMaxBMI(db) { min, max ->
                            minTezinaText = min
                            maxTezinaText = max
                        }
                    } else {
                        bmiNapredakTekst = "Unos nije ispravan."
                    }
                    isLoading = false
                }
            }) {
                Text("Ažuriraj napredak")
            }

            Spacer(modifier = Modifier.height(8.dp))


                Text(bmiNapredakTekst)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = maxTezinaText, fontWeight = FontWeight.Bold)
            Text(text = minTezinaText, fontWeight = FontWeight.Bold)


        }
    }
}
