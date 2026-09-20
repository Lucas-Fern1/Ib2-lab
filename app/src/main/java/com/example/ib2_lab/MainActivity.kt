package com.example.ib2_lab

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    // ==================================================
    // ESP32
    // ================================================ ==

    private val esp32IP = "192.168.4.1"

    // ==================================================
    // COMPONENTES
    // ==================================================

    private lateinit var txtVoltage: TextView
    private lateinit var txtADC: TextView
    private lateinit var txtFrequency: TextView
    private lateinit var txtStatus: TextView

    private lateinit var editFrequency: EditText
    private lateinit var btnApplyFrequency: Button

    private lateinit var graphView: GraphView

    // ==================================================
    // AMOSTRAS DO GRÁFICO
    // ==================================================

    private val samples =
        mutableListOf<Float>()

    // ==================================================
    // HANDLER
    // ==================================================

    private val handler =
        Handler(Looper.getMainLooper())

    // Evita várias requisições simultâneas
    @Volatile
    private var requestRunning = false

    // ==================================================
    // ATUALIZAÇÃO
    // ==================================================

    private val updateRunnable =
        object : Runnable {

            override fun run() {

                if (!requestRunning) {

                    requestRunning = true

                    getSamples()
                }

                handler.postDelayed(
                    this,
                    100
                )
            }
        }

    // ==================================================
    // ON CREATE
    // ==================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_main
        )

        // ==================================================
        // COMPONENTES
        // ==================================================

        txtVoltage =
            findViewById(
                R.id.txtVoltage
            )

        txtADC =
            findViewById(
                R.id.txtADC
            )

        txtFrequency =
            findViewById(
                R.id.txtFrequency
            )

        txtStatus =
            findViewById(
                R.id.txtStatus
            )

        editFrequency =
            findViewById(
                R.id.editFrequency
            )

        btnApplyFrequency =
            findViewById(
                R.id.btnApplyFrequency
            )

        graphView =
            findViewById(
                R.id.graphView
            )

        // ==================================================
        // BOTÃO FREQUÊNCIA
        // ==================================================

        btnApplyFrequency.setOnClickListener {

            val frequency =
                editFrequency.text
                    .toString()
                    .toIntOrNull()

            if (
                frequency != null &&
                frequency in 10..3000
            ) {

                setFrequency(
                    frequency
                )

            } else {

                txtStatus.text =
                    "Use uma frequência entre 10 e 3000 Hz"
            }
        }

        // ==================================================
        // INICIA AQUISIÇÃO
        // ==================================================

        handler.post(
            updateRunnable
        )
    }

    // ==================================================
    // RECEBE BLOCO DE AMOSTRAS
    // ==================================================

    private fun getSamples() {

        thread {

            try {

                // Primeiro usamos 100 amostras.
                val response =
                    httpGet(
                        "http://$esp32IP/samples?count=100"
                    )

                val json =
                    JSONObject(response)

                val frequency =
                    json.getInt(
                        "frequency"
                    )

                val jsonSamples =
                    json.getJSONArray(
                        "samples"
                    )

                val receivedSamples =
                    mutableListOf<Float>()

                for (
                i in 0 until jsonSamples.length()
                ) {

                    receivedSamples.add(
                        jsonSamples
                            .getDouble(i)
                            .toFloat()
                    )
                }

                runOnUiThread {

                    requestRunning = false

                    if (
                        receivedSamples.isNotEmpty()
                    ) {

                        // Guarda as amostras
                        samples.addAll(
                            receivedSamples
                        )

                        // Mantém no máximo 500 pontos
                        while (
                            samples.size > 500
                        ) {

                            samples.removeAt(0)
                        }

                        // Atualiza gráfico
                        graphView.setValues(
                            samples
                        )

                        // Última amostra
                        val lastVoltage =
                            receivedSamples.last()

                        val lastAdc =
                            (
                                    lastVoltage / 3.3f * 4095f
                                    ).toInt()

                        txtVoltage.text =
                            String.format(
                                "%.3f V",
                                lastVoltage
                            )

                        txtADC.text =
                            "ADC: $lastAdc"

                        txtFrequency.text =
                            "Taxa de amostragem: $frequency Hz"

                        txtStatus.text =
                            "ESP32 conectado"
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    requestRunning = false

                    txtStatus.text =
                        "ESP32 desconectado"
                }
            }
        }
    }

    // ==================================================
    // ALTERA FREQUÊNCIA
    // ==================================================

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

                    // Limpa o gráfico para começar
                    // a nova taxa de aquisição
                    samples.clear()

                    graphView.setValues(
                        samples
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    txtStatus.text =
                        "Erro ao alterar frequência"
                }
            }
        }
    }

    // ==================================================
    // HTTP GET
    // ==================================================

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
            2000

        connection.readTimeout =
            2000

        connection.connect()

        try {

            if (
                connection.responseCode !=
                HttpURLConnection.HTTP_OK
            ) {

                throw Exception(
                    "HTTP ${connection.responseCode}"
                )
            }

            return connection
                .inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

        } finally {

            connection.disconnect()
        }
    }

    // ==================================================
    // DESTRUIÇÃO
    // ==================================================

    override fun onDestroy() {

        handler.removeCallbacks(
            updateRunnable
        )

        super.onDestroy()
    }
}