package com.example.swand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.swand.ui.theme.SWandTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PositionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SWandTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        onConnectClick = { viewModel.connect(this) },
                        onDisconnectClick = { viewModel.disconnect(this) }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: PositionViewModel,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    val isConnected by viewModel.isConnected.observeAsState(false)
    val isButtonPressed by viewModel.isButtonPressed.observeAsState(false)
    val currentPosition by viewModel.currentPosition.observeAsState(Pair(0f, 0f))
    val drawingPath by viewModel.drawingPath.observeAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                if (isConnected) {
                    onDisconnectClick()
                } else {
                    onConnectClick()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (isConnected) "Disconnect" else "Connect to S Pen")
        }

        Text(
            text = "Status: ${if (isConnected) "Connected" else "Disconnected"}",
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Button: ${if (isButtonPressed) "PRESSED" else "Released"}",
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = "Position: X=${currentPosition.first}, Y=${currentPosition.second}",
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 16.dp)
        ) {
            DrawingGrid(
                currentPosition = currentPosition,
                drawingPath = drawingPath,
                isButtonPressed = isButtonPressed,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DrawingGrid(
    currentPosition: Pair<Float, Float>,
    drawingPath: List<Pair<Float, Float>>,
    isButtonPressed: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val scale = 70f // Увеличиваем масштаб для лучшего отображения

            // Рисуем сетку с меньшим шагом для эффекта "приближения"
            val gridSize = 10.dp.toPx() // Уменьшаем шаг сетки
            val gridColor = Color.LightGray.copy(alpha = 0.5f)

            // Вертикальные линии
            for (x in 0 until (size.width / gridSize).toInt() + 1) {
                val lineX = x * gridSize
                drawLine(
                    color = gridColor,
                    start = Offset(lineX, 0f),
                    end = Offset(lineX, size.height),
                    strokeWidth = 1f
                )
            }

            // Горизонтальные линии
            for (y in 0 until (size.height / gridSize).toInt() + 1) {
                val lineY = y * gridSize
                drawLine(
                    color = gridColor,
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 1f
                )
            }

            // Центральные оси (более заметные)
            drawLine(
                color = Color.Gray,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 3f
            )

            drawLine(
                color = Color.Gray,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 3f
            )

            // Рисуем путь движения (всегда, даже после отпускания кнопки)
            if (drawingPath.isNotEmpty()) {
                val pathPoints = drawingPath.map { point ->
                    Offset(
                        centerX + point.first * scale,
                        centerY - point.second * scale
                    )
                }

                // Рисуем линии между точками
                for (i in 0 until pathPoints.size - 1) {
                    drawLine(
                        color = if (isButtonPressed) Color.Blue else Color.Blue.copy(alpha = 0.7f),
                        start = pathPoints[i],
                        end = pathPoints[i + 1],
                        strokeWidth = if (isButtonPressed) 4f else 3f
                    )
                }

                // Рисуем точки пути
                for (point in pathPoints) {
                    drawCircle(
                        color = if (isButtonPressed) Color.Blue else Color.Blue.copy(alpha = 0.5f),
                        radius = if (isButtonPressed) 4f else 3f,
                        center = point
                    )
                }
            }

            // Рисуем текущую позицию только если кнопка нажата
            if (isButtonPressed) {
                val currentPoint = Offset(
                    centerX + currentPosition.first * scale,
                    centerY - currentPosition.second * scale
                )

                drawCircle(
                    color = Color.Red,
                    radius = 10f,
                    center = currentPoint,
                    style = Stroke(width = 3f)
                )

                drawCircle(
                    color = Color.Red.copy(alpha = 0.5f),
                    radius = 8f,
                    center = currentPoint
                )
            }
        }
    }
}