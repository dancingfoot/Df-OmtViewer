package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MedicalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ManualConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (name: String, host: String, port: Int) -> Unit
) {
    var streamName by remember { mutableStateOf("") }
    var hostIp by remember { mutableStateOf("192.168.1.") }
    var portText by remember { mutableStateOf("6400") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("manual_connect_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connect OMT Stream",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MedicalCyan,
                            fontSize = 18.sp
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_manual_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Text(
                    text = "Enter the IP address and TCP port of an Open Camera OMT sender, vMix, or OBS OMT output.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Quick preset for Open Camera on this phone
                OutlinedButton(
                    onClick = {
                        streamName = "Local Phone Camera (Open Camera)"
                        hostIp = "127.0.0.1"
                        portText = "6400"
                        isError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MedicalCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Use Local Phone Camera (127.0.0.1:6400)",
                        style = MaterialTheme.typography.labelMedium.copy(color = MedicalCyan)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = streamName,
                    onValueChange = { streamName = it },
                    label = { Text("Stream Label (Optional)") },
                    placeholder = { Text("e.g. Surgical Suite Cam 1") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MedicalCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_stream_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = hostIp,
                    onValueChange = {
                        hostIp = it
                        isError = false
                    },
                    label = { Text("Host / IP Address") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MedicalCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_host_ip")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("OMT Port (Default: 6400)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MedicalCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_port")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_connect_button")
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val cleanHost = hostIp.trim()
                            val port = portText.toIntOrNull() ?: 6400
                            if (cleanHost.isNotEmpty() && cleanHost != "192.168.1.") {
                                onConnect(streamName, cleanHost, port)
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MedicalCyan,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("confirm_connect_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Connect", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
