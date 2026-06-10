package com.g1.booklog.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.g1.booklog.data.repository.FriendInfo
import com.g1.booklog.data.repository.FriendRequest
import com.g1.booklog.ui.theme.ThinFriendsIcon
import com.g1.booklog.ui.viewmodel.AuthViewModel
import com.g1.booklog.ui.viewmodel.BookViewModel
import com.g1.booklog.ui.viewmodel.FriendViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    bookViewModel: BookViewModel,
    friendViewModel: FriendViewModel,
    onNavigateBack: () -> Unit,
    onFriendClick: (FriendInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val syncState by bookViewModel.syncState.collectAsState()
    val message by authViewModel.message.collectAsState()
    val currentNickname by authViewModel.nickname.collectAsState()
    val customPhotoUrl by authViewModel.customPhotoUrl.collectAsState()
    val friends by friendViewModel.friends.collectAsState()
    val friendRequests by friendViewModel.friendRequests.collectAsState()
    val friendMessage by friendViewModel.message.collectAsState()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var friendToRemove by remember { mutableStateOf<FriendInfo?>(null) }
    var selectedFriendTab by remember { mutableIntStateOf(0) }

    val user = FirebaseAuth.getInstance().currentUser

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { authViewModel.uploadProfileImage(context, it) } }

    LaunchedEffect(friendMessage) {
        if (friendMessage != null) {
            kotlinx.coroutines.delay(3000)
            friendViewModel.clearMessage()
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("로그아웃") },
            text = { Text("로그아웃 하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.signOut(context)
                    showSignOutDialog = false
                    onNavigateBack()
                }) { Text("로그아웃", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("취소") } }
        )
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("클라우드 업로드") },
            text = { Text("현재 기기의 책 데이터를 클라우드에 저장합니다.\n기존 클라우드 데이터는 덮어씌워집니다.") },
            confirmButton = {
                Button(onClick = { bookViewModel.uploadToCloud(); showUploadDialog = false }) { Text("업로드") }
            },
            dismissButton = { TextButton(onClick = { showUploadDialog = false }) { Text("취소") } }
        )
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("클라우드 다운로드") },
            text = { Text("클라우드에 저장된 책 데이터를 불러옵니다.\n현재 기기의 데이터는 모두 교체됩니다.") },
            confirmButton = {
                Button(onClick = { bookViewModel.downloadFromCloud(); showDownloadDialog = false }) { Text("다운로드") }
            },
            dismissButton = { TextButton(onClick = { showDownloadDialog = false }) { Text("취소") } }
        )
    }

    if (showNicknameDialog) {
        NicknameDialog(
            current = currentNickname,
            onConfirm = { nick -> authViewModel.updateNickname(nick); showNicknameDialog = false },
            onDismiss = { showNicknameDialog = false }
        )
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            onConfirm = { email ->
                friendViewModel.sendFriendRequest(email)
                showAddFriendDialog = false
            },
            onDismiss = { showAddFriendDialog = false }
        )
    }

    friendToRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendToRemove = null },
            title = { Text("친구 삭제") },
            text = { Text("\"${friend.visibleName()}\"을(를) 친구 목록에서 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    friendViewModel.removeFriend(friend.uid)
                    friendToRemove = null
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { friendToRemove = null }) { Text("취소") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 프로필 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            val displayPhoto = customPhotoUrl.ifBlank { user?.photoUrl?.toString() ?: "" }
                            if (displayPhoto.isNotEmpty()) {
                                AsyncImage(
                                    model = displayPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(64.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Person, null,
                                            modifier = Modifier.size(36.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            SmallFloatingActionButton(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.size(24.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(Icons.Default.CameraAlt, "사진 변경", modifier = Modifier.size(14.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentNickname.ifBlank { user?.displayName ?: "사용자" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(onClick = { showNicknameDialog = true }) {
                            Icon(Icons.Default.Edit, "별명 수정", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                message?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.contains("실패")) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }

                // 데이터 동기화
                Text(
                    "데이터 동기화",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                syncState?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.contains("실패")) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showUploadDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("업로드")
                    }
                    Button(
                        onClick = { showDownloadDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("다운로드")
                    }
                }

                // 친구 섹션 (인라인)
                FriendSectionCard(
                    friends = friends,
                    requests = friendRequests,
                    message = friendMessage,
                    selectedTab = selectedFriendTab,
                    onTabSelect = { selectedFriendTab = it },
                    onAddFriend = { showAddFriendDialog = true },
                    onFriendClick = onFriendClick,
                    onFriendLongClick = { friendToRemove = it },
                    onAcceptRequest = { friendViewModel.acceptRequest(it) },
                    onDeclineRequest = { friendViewModel.declineRequest(it.requestId) }
                )

                OutlinedButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("로그아웃")
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FriendSectionCard(
    friends: List<FriendInfo>,
    requests: List<FriendRequest>,
    message: String?,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onAddFriend: () -> Unit,
    onFriendClick: (FriendInfo) -> Unit,
    onFriendLongClick: (FriendInfo) -> Unit,
    onAcceptRequest: (FriendRequest) -> Unit,
    onDeclineRequest: (FriendRequest) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onTabSelect(0) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        ThinFriendsIcon, null,
                        modifier = Modifier.size(18.dp),
                        tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "친구 (${friends.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                TextButton(
                    onClick = { onTabSelect(1) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val badge = if (requests.isNotEmpty()) " (${requests.size})" else ""
                    Text(
                        "친구 요청$badge",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onAddFriend,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Add, "친구 추가",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            message?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (msg.contains("실패") || msg.contains("없") || msg.contains("이미") || msg.contains("자기"))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                0 -> FriendInlineGrid(
                    friends = friends,
                    onFriendClick = onFriendClick,
                    onFriendLongClick = onFriendLongClick
                )
                1 -> RequestsInlineList(
                    requests = requests,
                    onAccept = onAcceptRequest,
                    onDecline = onDeclineRequest
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendInlineGrid(
    friends: List<FriendInfo>,
    onFriendClick: (FriendInfo) -> Unit,
    onFriendLongClick: (FriendInfo) -> Unit
) {
    if (friends.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "아직 친구가 없어요\n+ 버튼으로 친구를 추가해보세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        val columns = 4
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            friends.chunked(columns).forEach { rowFriends ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowFriends.forEach { friend ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { onFriendClick(friend) },
                                    onLongClick = { onFriendLongClick(friend) }
                                )
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FriendAvatar(name = friend.visibleName(), photoUrl = friend.photoUrl, size = 48.dp)
                            Text(
                                text = friend.visibleName(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    repeat(columns - rowFriends.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsInlineList(
    requests: List<FriendRequest>,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "받은 친구 요청이 없습니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            requests.forEach { req ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FriendAvatar(name = req.visibleName(), photoUrl = req.fromPhotoUrl, size = 36.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = req.visibleName(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = req.fromEmail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = { onAccept(req) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("수락", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = { onDecline(req) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("거절", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun NicknameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("별명 설정") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("별명") },
                placeholder = { Text("친구에게 보여질 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }, enabled = value.isNotBlank()) {
                Text("저장")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
