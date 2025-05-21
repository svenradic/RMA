package com.example.predlozak_1

import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.floatPreferencesKey
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlin.math.sqrt
import kotlin.random.Random

val Context.dataStore by preferencesDataStore(name = "user_prefs")
val REZULTAT_KEY = stringPreferencesKey("rezultat_bmi")
suspend fun spremiRezultat(context: Context, tekst: String) {
    context.dataStore.edit { preferences ->
        preferences[REZULTAT_KEY] = tekst
    }
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
            val navController = rememberNavController()
            Predlozak_1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(navController = navController, startDestination = "main_screen",
                        modifier = Modifier.padding(innerPadding)
                    ) { composable("main_screen") {
                        MainScreen(navController = navController)
                    }
                        composable("step_counter") {
                            StepCounter(navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserPreview(heightCm: Int, weightKg: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    context.dataStore
    val db = FirebaseFirestore.getInstance()

    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val rezultat by dohvatiRezultat(context).collectAsState(initial = "")
    var progress by remember { mutableStateOf(0f) }
    var newWeightInput by remember { mutableStateOf("") }
    var bmiNapredakTekst by remember { mutableStateOf("") }

    val heightMeters = heightCm / 100f
    val bmi = weightKg / (heightMeters * heightMeters)
    val idealBmi = 21.7f

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
                    val rezultat = "Udaljeni ste %.1f od idealnog BMI-a.".format(razlika)

                    spremiRezultat(context, rezultat)
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
                    } else {
                        bmiNapredakTekst = "Unos nije ispravan."
                    }
                    isLoading = false
                }
            }) {
                Text("Ažuriraj napredak")
            }
            Button(onClick = {
                db.collection("BMI").get()
                    .addOnSuccessListener { documents ->
                        val tezine = documents.mapNotNull { it.getDouble("Tezina") }
                        val visine = documents.mapNotNull { it.getDouble("Visina") }

                        if (tezine.isNotEmpty() && visine.isNotEmpty()) {
                            val maxTezina = tezine.maxOrNull()!!
                            val minTezina = tezine.minOrNull()!!
                            val visina = visine.first() / 100.0  // koristi prvu visinu kao pretpostavku

                            val maxBmi = maxTezina / (visina * visina)
                            val minBmi = minTezina / (visina * visina)

                            bmiNapredakTekst =
                                "Najveća težina: %.1f kg (BMI: %.1f)\nNajmanja težina: %.1f kg (BMI: %.1f)"
                                    .format(maxTezina, maxBmi, minTezina, minBmi)
                        } else {
                            bmiNapredakTekst = "Nema dovoljno podataka u bazi."
                        }
                    }
                    .addOnFailureListener {
                        bmiNapredakTekst = "Greška pri dohvaćanju podataka."
                    }
            }) {
                Text("Analiziraj podatke")
            }

            Spacer(modifier = Modifier.height(8.dp))


                Text(bmiNapredakTekst)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                )


        }
    }
}

@Composable
fun MainScreen(navController: NavController, modifier: Modifier = Modifier) {
    Box( modifier = modifier
        .fillMaxSize()){
        UserPreview(
            heightCm = 191,
            weightKg = 100,
            modifier = Modifier.fillMaxSize()
        )
        Button(
            onClick = { navController.navigate("step_counter") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Idi na brojač koraka")
        }}
}

@Composable
fun StepCounter(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val stepCount = remember { mutableStateOf(0) }
    var lastMagnitude = remember { 0f }
    var stepsAtLastSave = remember { 0 }

    // Firebase
    val firestore = Firebase.firestore

    // Notifikacijski kanal (pozovi jednom iz MainActivity)
    LaunchedEffect(Unit) {
        createNotificationChannel(context)
    }

    DisposableEffect(Unit) {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val magnitude = sqrt(x * x + y * y + z * z)
                val delta = magnitude - lastMagnitude

                if (delta > 12f) {
                    stepCount.value++

                    // Notifikacija + Firebase nakon svakih 50 koraka
                    if (stepCount.value % 50 == 0 && stepCount.value != stepsAtLastSave) {
                        stepsAtLastSave = stepCount.value

                        // Lok. notifikacija
                        showNotification(
                            context,
                            "Bravo!",
                            "Napravili ste više od ${stepCount.value} koraka!"
                        )

                        // Spremi u Firestore
                        val koraciData = hashMapOf(
                            "koraci" to stepCount.value,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                        firestore.collection("Koraci")
                            .add(koraciData)
                    }
                }

                lastMagnitude = magnitude
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.fitness),
            contentDescription = "Pozadinska slika",
            contentScale = ContentScale.Crop,
            alpha = 0.1f,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Broj koraka: ${stepCount.value}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("main_screen") },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Idi na glavni ekran")
            }
        }
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

fun showNotification(context: Context, title: String, message: String) {
    if (!hasNotificationPermission(context)) return

    val builder = NotificationCompat.Builder(context, "koraci_channel")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    with(NotificationManagerCompat.from(context)) {
        notify(Random.nextInt(), builder.build())
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Koraci kanal"
        val descriptionText = "Obavijesti o broju koraka"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("koraci_channel", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}


