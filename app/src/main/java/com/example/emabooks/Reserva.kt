package com.example.emabooks

import com.google.firebase.Timestamp

enum class StatusReserva {
    ATIVA, CANCELADA, CONVERTIDA, PENDENTE
}

data class Reserva(
    val id: String = "",
    val usuarioId: String = "",
    val livroId: String = "",
    val dataReserva: Timestamp? = null,
    val expiraEm: Timestamp? = null,
    val status: StatusReserva = StatusReserva.PENDENTE,
    val posicaoFila: Int = 1
)