
package com.example.yatrimitra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Vehicle(val id:Int,val position:Float)
data class UiState(val vehicles:List<Vehicle>, val eta:Int)

class YatriMitraViewModel: ViewModel() {

 private val _ui = MutableStateFlow(
  UiState(listOf(Vehicle(1,0f),Vehicle(2,.4f),Vehicle(3,.7f)),60)
 )
 val uiState:StateFlow<UiState> = _ui

 init { simulate() }

 /**
  * Simulation Logic
  * ETA = Distance / Speed
  * State survives rotation because ViewModel survives configuration changes.
  */
 private fun simulate() {
  viewModelScope.launch {
   var p1=0f; var p2=.4f; var p3=.7f
   val speed=.003f

   while(true){
    p1=(p1+speed)%1f
    p2=(p2+speed)%1f
    p3=(p3+speed)%1f

    val eta=((1f-p1)/speed).toInt()

    _ui.value=UiState(
      listOf(
        Vehicle(1,p1),
        Vehicle(2,p2),
        Vehicle(3,p3)
      ),
      eta
    )
    delay(50)
   }
  }
 }
}
