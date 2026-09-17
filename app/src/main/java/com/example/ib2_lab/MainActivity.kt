package com.example.ib2_lab

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // ==========================================
    // IP DO ESP32
    // ==========================================

    private val esp32IP = "192.168.4.1"

    // ==========================================
    // COMPONENTES DA TELA
    // ==========================================

    private lateinit var txtVoltage: TextView
    private lateinit var txtADC: TextView
    private lateinit var txtFrequency: TextView
    private lateinit var txtStatus: TextView

    private lateinit var editFrequency: EditText
    private lateinit var btnApplyFrequency: Button

    private lateinit var graphView: GraphView

    // ==========================================
    // DADOS DO GRÁFICO
    // ==========================================

    private val samples = mutableListOf<Float>()

    // ==========================================
    // CICLO DE ATUALIZAÇÃO
    // ==========================================

    private val updateRunnable = object : Runnable {

        override fun run() {

            getData()

            android.os.Handler(
                mainLooper
            ).postDelayed(
                this,
                100
            )
        }
    }

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        // ======================================
        // LIGA COMPONENTES DO XML
        // ======================================

        txtVoltage =
            findViewById(R.id.txtVoltage)

        txtADC =
            findViewById(R.id.txtADC)

        txtFrequency =
            findViewById(R.id.txtFrequency)

        txtStatus =
            findViewById(R.id.txtStatus)

        editFrequency =
            findViewById(R.id.editFrequency)

        btnApplyFrequency =
            findViewById(R.id.btnApplyFrequency)

        graphView =
            findViewById(R.id.graphView)

        // ======================================
        // BOTÃO DE FREQUÊNCIA
        // ======================================

        btnApplyFrequency.setOnClickListener {

            val frequency =
                editFrequency.text
                    .toString()
                    .toIntOrNull()

            if (
                frequency != null &&
                frequency in 10..3000
            ) {

                setFrequency(frequency)

            } else {

                txtStatus.text =
                    "Use uma frequência entre 10 e 3000 Hz"
            }
        }

        // ======================================
        // COMEÇA ATUALIZAÇÃO
        // ======================================

        android.os.Handler(
            mainLooper
        ).post(updateRunnable)
    }

    // ==========================================
    // RECEBE DADOS DO ESP32
    // ==========================================

    private fun getData() {

        thread {

            try {

                val response =
                    httpGet(
                        "http://$esp32IP/data"
                    )

                val json =
                    JSONObject(response)

                val adc =
                    json.getInt("adc")

                val voltage =
                    json.getDouble("voltage")

                val frequency =
                    json.getInt("frequency")

                runOnUiThread {

                    txtVoltage.text =
                        String.format(
                            "%.3f V",
                            voltage
                        )

                    txtADC.text =
                        "ADC: $adc"

                    txtFrequency.text =
                        "Taxa de amostragem: $frequency Hz"

                    txtStatus.text =
                        "ESP32 conectado"

                    // --------------------------------
                    // Adiciona amostra ao gráfico
                    // --------------------------------

                    samples.add(
                        voltage.toFloat()
                    )

                    // Mantém somente 200 pontos
                    if (samples.size > 200) {

                        samples.removeAt(0)
                    }

                    graphView.setValues(
                        samples
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    txtStatus.text =
                        "ESP32 desconectado"
                }
            }
        }
    }

    // ==========================================
    // ALTERA FREQUÊNCIA NO ESP32
    // ==========================================

    private fun setFrequency(
        frequency: Int
    ) {

        thread {

            try {

                httpGet(
                    "http://$esp32IP/setFrequency?value=$frequency"
                )

                runOnUiThread {

                    txtStatus.text =
                        "Frequência alterada para $frequency Hz"
                }

            } catch (e: Exception) {

                runOnUiThread {

                    txtStatus.text =
                        "Erro ao alterar frequência"
                }
            }
        }
    }

    // ==========================================
    // REQUISIÇÃO HTTP
    // ==========================================

    private fun httpGet(
        address: String
    ): String {

        val url =
            URL(address)

        val connection =
            url.openConnection()
                    as HttpURLConnection

        connection.requestMethod =
            "GET"

        connection.connectTimeout =
            1000

        connection.readTimeout =
            1000

        connection.connect()

        return connection
            .inputStream
            .bufferedReader()
            .use {
                it.readText()
            }
    }

    // ==========================================
    // ENCERRA ATUALIZAÇÃO
    // ==========================================

    override fun onDestroy() {

        super.onDestroy()

        android.os.Handler(
            mainLooper
        ).removeCallbacks(
            updateRunnable
        )
    }
}