package com.example.testproject

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testproject.ui.theme.TestProjectTheme


class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestProjectTheme {

               Surface(
                    modifier = Modifier.fillMaxSize(),

                ) {

                   run {

                       DisplaySensors()
                   }
                }
            }
        }
    }
}
@Preview
@Composable
fun DisplaySensors() {
    // on below line we are creating
    // a variable for a context
    val ctx = LocalContext.current

    // on below line we are creating a column
    Column(

        modifier = Modifier
            .fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(7.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // on below line we are initializing our sensor manager.
        val sensorManager: SensorManager =
            ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // on below line we are creating list for device sensors.
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)


        // on below line we are creating a simple text
        // in which we are displaying a text as Object is
        Text(
            text = "Sensors in Devices are : ",

            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
            fontSize = 20.sp, modifier = Modifier.padding(5.dp)
        )

        // on below line creating a variable for sensor data.
        var sensorsData = ""

        // on below line adding all sensors from
        // device sensors in our variable.
        for (sens in deviceSensors) {
            sensorsData = sensorsData + sens.name + " \n"
        }

        Text(
            text = sensorsData,

            color = Color.Black,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            modifier = Modifier.padding(5.dp)
        )

    }

}
