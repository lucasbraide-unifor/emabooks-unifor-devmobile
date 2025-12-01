package com.example.emabooks

import com.google.firebase.Timestamp
import java.io.Serializable

enum class TipoAcervo { FISICO, DIGITAL, HIBRIDO }
enum class StatusLivro { DISPONIVEL, INDISPONIVEL, RESERVADO, ESGOTADO, PENDENTE}

data class Livro(
    val id: String = "",
    val titulo: String = "",
    val autor: String? = null,
    val anoPublicacao: Int? = null,
    val editora: String? = null,
    val tipoAcervo: TipoAcervo = TipoAcervo.FISICO,
    val categorias: List<String> = emptyList(),
    val descricao: String? = null,
    val capaUrl: String? = null,
    val statusGeral: StatusLivro = StatusLivro.DISPONIVEL,
    val urlAcessoDigital: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) : Serializable