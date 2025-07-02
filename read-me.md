1 - MainActivity

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

            // Configurações de som e vibração
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

2 - ScanResultManager.kt

package com.austin.kjetScanner.printer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ScanResultManager {
    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult

    fun updateScanResult(type: String, value: String) {
        _scanResult.value = ScanResult(type, value, System.currentTimeMillis())
    }

    fun clearScanResult() {
        _scanResult.value = null
    }
}

data class ScanResult(
    val type: String,
    val value: String,
    val timestamp: Long
)


3 - SunmiBarcodeScannerScreen.kt

package com.austin.kjetScanner.printer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SunmiBarcodeScannerScreen(
    onStartScan: () -> Unit
) {
    // Observar mudanças no ScanResultManager
    val scanResult by ScanResultManager.scanResult.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Ícone do Scanner
            Card(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(60.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanner",
                        modifier = Modifier.size(60.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Título
            Text(
                text = "Scanner Sunmi",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )



            Spacer(modifier = Modifier.height(48.dp))



            // Botão de Scan
            Button(
                onClick = onStartScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Iniciar Scanner",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Resultado do último scan (se houver)
            scanResult?.let { result ->
                LastScanResultCard(
                    scanResult = result,
                    onClear = { ScanResultManager.clearScanResult() },
                    onCopy = { value ->
                        clipboardManager.setText(AnnotatedString(value))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Informações sobre formatos suportados
        SupportedFormatsInfo(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun LastScanResultCard(
    scanResult: ScanResult,
    onClear: () -> Unit,
    onCopy: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val formattedDate = remember(scanResult.timestamp) {
        dateFormat.format(Date(scanResult.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Último Scan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Row {
                    IconButton(
                        onClick = { onCopy(scanResult.value) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    IconButton(
                        onClick = onClear
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpar",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Valor do código
            Text(
                text = "Código:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            SelectionContainer {
                Text(
                    text = scanResult.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Tipo do código
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tipo:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = scanResult.type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Data/Hora do scan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Data:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divisor
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))


        }
    }
}

@Composable
fun SupportedFormatsInfo(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Formatos Suportados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val formats = listOf(
                "QR Code", "EAN-8", "EAN-13", "UPC-A", "UPC-E",
                "Code 128", "Code 39", "Code 93", "Codabar",
                "PDF417", "Data Matrix", "Aztec", "Micro PDF417"
            )

            Text(
                text = formats.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )
        }
    }
}


4 - Androidmanifest.xml

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">



    <!-- Permissões necessárias para o scanner -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <!-- Permissões para impressora Sunmi -->
    <uses-permission android:name="com.sunmi.permission.PRINTER_SERVICE" />
    <uses-permission android:name="com.sunmi.permission.SCANNER_SERVICE" />

    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="com.sunmi.permission.SCANNER" />

    <uses-permission android:name="com.sunmi.permission.PRINTER" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <uses-feature
        android:name="android.hardware.camera"
        android:required="true" />
    <uses-feature
        android:name="android.hardware.camera.autofocus"
        android:required="false" />
    <!-- Características de hardware -->
    <!-- Adicione se necessário para o SDK da Sunmi -->
    <queries>
        <intent>
            <action android:name="com.summi.scan" />
        </intent>
    </queries>

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.KJetPos"
        tools:targetApi="31">

        <activity
            android:name="MainActivity"
            android:exported="true"
            android:theme="@style/Theme.KJetPos"
            tools:ignore="MissingClass">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>


5 - dependencies buil.gradle (Module: app)
 implementation("com.sunmi:printerlibrary:1.0.24")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")
    // ML Kit para reconhecimento de códigos de barras
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.5.8")
    implementation("androidx.camera:camera-camera2:1.3.1")




