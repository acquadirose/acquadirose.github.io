@file:OptIn(ExperimentalLayoutApi::class)

package it.agenda.estetista.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.PostIt
import it.agenda.estetista.util.dataMedia
import java.time.LocalDate

/** Colori dei post-it: giallo, rosa, acqua, glicine. */
val PostItColors = listOf(
    Color(0xFFFFF1B8),
    Color(0xFFF9D6E0),
    Color(0xFFD4EEEE),
    Color(0xFFE6DDF3),
)

fun postItColor(i: Int) = PostItColors[i.mod(PostItColors.size)]

private fun PostIt.whenLabel(): String =
    date?.let { "Solo " + dataMedia(LocalDate.ofEpochDay(it)) } ?: "Sempre visibile"

/** Striscia di post-it in cima all'agenda (solo quelli da fare). */
@Composable
fun PostItStrip(vm: AgendaViewModel, onOpen: () -> Unit) {
    val all by vm.postIts.collectAsStateWithLifecycle()
    val open = all.filter { !it.done }
    if (open.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        open.forEach { p ->
            Column(
                Modifier
                    .width(220.dp)
                    .heightIn(min = 64.dp)
                    .shadow(2.dp, RoundedCornerShape(4.dp))
                    .background(postItColor(p.color), RoundedCornerShape(4.dp))
                    .clickable(onClick = onOpen)
                    .padding(10.dp),
            ) {
                Text(p.text, fontSize = 14.sp, color = Rose.Ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (p.date == null) "Fisso" else "Questo giorno",
                    fontSize = 11.sp, color = Rose.Muted,
                )
            }
        }
    }
}

@Composable
fun PostItDialog(vm: AgendaViewModel, day: LocalDate, onDismiss: () -> Unit) {
    val list by vm.postIts.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<PostIt?>(null) }
    var text by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0) }
    var pinned by remember { mutableStateOf(false) }

    fun reset() { editing = null; text = ""; color = 0; pinned = false }

    val dirty = text.isNotBlank() && text.trim() != editing?.text?.trim()
    var askDiscard by remember { mutableStateOf(false) }
    fun tryClose() { if (dirty) askDiscard = true else onDismiss() }
    fun edit(p: PostIt) { editing = p; text = p.text; color = p.color; pinned = p.date == null }

    Dialog(
        onDismissRequest = { tryClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxWidth(0.93f)
                .fillMaxHeight(0.9f),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Post-it", style = SerifTitle.copy(fontSize = 28.sp))
                        Text(dataMedia(day).replaceFirstChar { it.uppercase() }, color = Rose.Muted)
                    }
                    IconButton(onClick = { tryClose() }) { Icon(Icons.Filled.Close, "Chiudi") }
                }

                // ── Scrivi ──
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(postItColor(color), RoundedCornerShape(6.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        placeholder = { Text("Scrivi una nota… es. ordinare cera, richiamare Giulia, consegna prodotti") },
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PostItColors.forEachIndexed { i, c ->
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (i == color) 3.dp else 1.dp, if (i == color) Rose.Ink else Rose.HourLine, CircleShape)
                                    .clickable { color = i },
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        FilterChip(selected = !pinned, onClick = { pinned = false }, label = { Text("Solo ${dataMedia(day)}") })
                        FilterChip(selected = pinned, onClick = { pinned = true }, label = { Text("Sempre visibile") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                val base = editing ?: PostIt(text = "")
                                vm.savePostIt(
                                    base.copy(
                                        text = text,
                                        color = color,
                                        date = if (pinned) null else (editing?.date ?: day.toEpochDay()),
                                    )
                                )
                                reset()
                            },
                            enabled = text.isNotBlank(),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (editing == null) "Attacca post-it" else "Aggiorna")
                        }
                        if (editing != null) TextButton(onClick = { reset() }) { Text("Annulla modifica") }
                    }
                }

                // ── Bacheca ──
                if (list.isEmpty()) {
                    Text("Nessun post-it per questo giorno.", color = Rose.Muted)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    list.forEach { p ->
                        Column(
                            Modifier
                                .width(260.dp)
                                .heightIn(min = 150.dp)
                                .shadow(3.dp, RoundedCornerShape(4.dp))
                                .background(postItColor(p.color), RoundedCornerShape(4.dp))
                                .alpha(if (p.done) 0.55f else 1f)
                                .clickable { edit(p) }
                                .padding(14.dp),
                        ) {
                            Text(
                                p.text,
                                fontSize = 15.sp,
                                color = Rose.Ink,
                                textDecoration = if (p.done) TextDecoration.LineThrough else null,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(p.whenLabel(), fontSize = 12.sp, color = Rose.Muted, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = p.done, onCheckedChange = { vm.savePostIt(p.copy(done = it)) })
                                Text("Fatto", fontSize = 13.sp)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { vm.deletePostIt(p); if (editing?.id == p.id) reset() }) {
                                    Icon(Icons.Filled.Delete, "Elimina post-it", tint = Rose.Muted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (askDiscard) {
        DiscardDialog(onKeep = { askDiscard = false }, onDiscard = { askDiscard = false; onDismiss() })
    }
}
