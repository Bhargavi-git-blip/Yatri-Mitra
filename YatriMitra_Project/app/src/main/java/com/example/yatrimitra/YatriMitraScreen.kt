
package com.example.yatrimitra

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun YatriMitraScreen(vm:YatriMitraViewModel){
 val state by vm.uiState.collectAsStateWithLifecycle()

 Column(Modifier.fillMaxSize().padding(16.dp)){
   Text("Yatri-Mitra", style = MaterialTheme.typography.headlineMedium)
   Spacer(Modifier.height(8.dp))
   Text("Next Auto ETA: ${state.eta} sec")

   Spacer(Modifier.height(24.dp))

   Canvas(
      modifier = Modifier.fillMaxWidth().height(250.dp)
   ){
      val y=size.height/2

      drawLine(
        start=Offset(50f,y),
        end=Offset(size.width-50f,y)
      )

      repeat(5){
        val x=50f + ((size.width-100f)/4f)*it
        drawCircle(radius=10f, center=Offset(x,y))
      }

      state.vehicles.forEach{
        drawCircle(
          radius=20f,
          center=Offset(
            50f + (size.width-100f)*it.position,
            y-40f
          )
        )
      }
   }
 }
}
