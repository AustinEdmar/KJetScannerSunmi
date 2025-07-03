package com.austin.kjetScanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.austin.kjetScanner.printer.SunmiBarcodeScannerScreen
import com.austin.kjetScanner.printer.ScanResultManager
import com.austin.kjetScanner.ui.theme.SunmiPrinterTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val START_SCAN = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SunmiPrinterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SunmiBarcodeScannerScreen(
                        onStartScan = { startSunmiScanner() }
                    )
                }
            }
        }
    }

    private fun startSunmiScanner() {
        try {
            // Configuração baseada no SDK fornecido
            val intent = Intent("com.summi.scan")

            // Configurações de som e vibrato
            intent.putExtra("PLAY_SOUND", true) // Som após completar o scan
            intent.putExtra("PLAY_VIBRATE", false) // Vibração (suportado no M1, não no V1)

            // Configurações de interface
            intent.putExtra("IS_SHOW_SETTING", true) // Botão de configurações
            intent.putExtra("IS_SHOW_ALBUM", true) // Botão para selecionar da galeria
            intent.putExtra("IS_OPEN_LIGHT", true) // Exibir lanterna
            intent.putExtra("SCAN_MODE", false) // Modo de ciclo

            // Configurações de reconhecimento
            intent.putExtra("IDENTIFY_MORE_CODE", false) // Múltiplos QR codes na imagem
            intent.putExtra("IDENTIFY_INVERSE", true) // QR codes com cores inversas

            // Formatos de código de barras habilitados
            intent.putExtra("IS_QR_CODE_ENABLE", true) // QR Code
            intent.putExtra("IS_EAN_8_ENABLE", true) // EAN-8
            intent.putExtra("IS_UPC_E_ENABLE", true) // UPC-E
            intent.putExtra("IS_ISBN_10_ENABLE", false) // ISBN-10
            intent.putExtra("IS_CODE_11_ENABLE", true) // CODE-11
            intent.putExtra("IS_UPC_A_ENABLE", true) // UPC-A
            intent.putExtra("IS_EAN_13_ENABLE", true) // EAN-13
            intent.putExtra("IS_ISBN_13_ENABLE", true) // ISBN-13
            intent.putExtra("IS_INTERLEAVED_2_OF_5_ENABLE", true) // Interleaved 2 of 5
            intent.putExtra("IS_CODE_128_ENABLE", true) // Code 128
            intent.putExtra("IS_CODABAR_ENABLE", true) // Codabar
            intent.putExtra("IS_CODE_39_ENABLE", true) // Code 39
            intent.putExtra("IS_CODE_93_ENABLE", true) // Code 93
            intent.putExtra("IS_DATABAR_ENABLE", true) // DataBar (RSS-14)
            intent.putExtra("IS_DATABAR_EXP_ENABLE", true) // DataBar Expanded
            intent.putExtra("IS_Micro_PDF417_ENABLE", true) // Micro PDF417
            intent.putExtra("IS_MicroQR_ENABLE", true) // Micro QR Code
            intent.putExtra("IS_PDF417_ENABLE", true) // PDF417
            intent.putExtra("IS_DATA_MATRIX_ENABLE", true) // DataMatrix
            intent.putExtra("IS_AZTEC_ENABLE", true) // AZTEC
            intent.putExtra("IS_Hanxin_ENABLE", false) // Hanxin

            startActivityForResult(intent, START_SCAN)
        } catch (e: Exception) {
            // Fallback caso o SDK da Sunmi não esteja disponível
            android.util.Log.e("SunmiScanner", "Erro ao iniciar scanner Sunmi", e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == START_SCAN && data != null) {
            val bundle = data.extras
            val result = bundle?.getSerializable("data") as? ArrayList<HashMap<String, String>>

            result?.let { scanResults ->
                val iterator = scanResults.iterator()
                while (iterator.hasNext()) {
                    val hashMap = iterator.next()
                    val type = hashMap["TYPE"] ?: "Desconhecido"
                    val value = hashMap["VALUE"] ?: ""

                    android.util.Log.i("sunmi", "Tipo: $type")
                    android.util.Log.i("sunmi", "Valor: $value")

                    // Processar resultado do scan e atualizar o manager
                    processScanResult(type, value)
                }
            }
        }
    }

    private fun processScanResult(type: String, value: String) {
        // Atualizar o ScanResultManager para que a UI seja notificada
        ScanResultManager.updateScanResult(type, value)

        // Log para debug
        android.util.Log.i("ScanResult", "Resultado atualizado - Tipo: $type, Valor: $value")
    }
}