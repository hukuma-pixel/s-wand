package com.example.swand

class PositionManager {
    private var x :Float = 0f
    private var y :Float = 0f

    fun changePosition(deltaX : Float, deltaY : Float)
    {
        x += deltaX
        y += deltaY
    }

    fun resetPosition()
    {
        x = 0f
        y = 0f
    }

    fun getX() : Float = x
    fun getY() : Float = y

}