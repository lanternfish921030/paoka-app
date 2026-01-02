package com.example.a0725.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.a0725.navigation.Routes
import com.example.a0725.navigation.navigateSingleTopTo

// ─────────────────────────────────────────────
// 品牌色票定義
// ─────────────────────────────────────────────
private val BrandYellow      = Color(0xFFFFA800)   // 主要強調色 (黃)
private val BrandTextMain      = Color(0xFF333333) // 主要文字（深黑灰，比之前更深一點以符合"黑色"需求）
private val BrandTextSecondary = Color(0xFF9E9E9E) // 次要文字

// 底色
private val BrandSurface        = Color(0xFFFFFFFF) // 白色 (背景)
private val BrandSurfaceVariant = Color(0xFFF8F8F8) // 極淺灰 (卡片，稍微調亮一點點讓黃色更跳)


// ─────────────────────────────────────────────
// 「其他」主頁
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherScreen(nav: NavHostController) {
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "其他",
                        fontSize = 25.sp,

                        color = BrandTextMain
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BrandSurface
                )
            )
        },
        containerColor = BrandSurface
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SettingsSectionTitle("關於本 APP") }
            item { ChevronRow("開發人員") { nav.navigateSingleTopTo(Routes.ABOUT_DEVS) } }
            item { ChevronRow("資料來源") { nav.navigateSingleTopTo(Routes.DATA_SOURCES) } }

            item { SettingsSectionTitle("支援與說明") }
            item { ChevronRow("問題回報") { nav.navigateSingleTopTo(Routes.FEEDBACK) } }
            item { ChevronRow("常見問題") { nav.navigateSingleTopTo(Routes.FAQ) } }

            item { SettingsSectionTitle("條款與隱私") }
            item { ChevronRow("服務條款") { nav.navigateSingleTopTo(Routes.TERMS) } }
            item { ChevronRow("隱私政策") { nav.navigateSingleTopTo(Routes.PRIVACY_POLICY) } }
            item { ChevronRow("聯絡我們") { nav.navigateSingleTopTo(Routes.CONTACT) } }
        }
    }
}

// 小標題 (帶有黃色裝飾線)
@Composable
private fun SettingsSectionTitle(text: String) {
    Row(
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 黃色小豎線
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BrandYellow)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BrandTextSecondary
        )
    }
}

// 主選單的卡片按鈕
@Composable
private fun ChevronRow(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        color = BrandSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp), // 稍微增加一點高度更有質感
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = BrandTextMain,
                modifier = Modifier.weight(1f)
            )
            // ▼▼▼ 修改處：箭頭改回黑色 (深灰) ▼▼▼
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = BrandTextMain, // 使用深色
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// 子頁 1：開發人員 (增加黃色圖示)
// ─────────────────────────────────────────────

@Composable
fun AboutDevsScreen(nav: NavHostController) {
    InfoScaffold(title = "開發人員", nav = nav) {

        InfoCard {
            // ▼▼▼ 新增：標題帶黃色圖示 ▼▼▼
            YellowIconTitle(icon = Icons.Outlined.Group, title = "開發團隊")
            Spacer(Modifier.height(12.dp))
            Text(
                text = "咖灰杯 團隊",
                fontSize = 18.sp, // 稍微加大團隊名稱
                fontWeight = FontWeight.SemiBold,
                color = BrandTextMain,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// 子頁 2：資料來源 (增加黃色圖示分類)
// ─────────────────────────────────────────────

@Composable
fun DataSourcesScreen(nav: NavHostController) {
    InfoScaffold(title = "資料來源", nav = nav) {
        InfoCard {
            // ▼▼▼ 新增：標題帶黃色圖示 ▼▼▼
            YellowIconTitle(Icons.Outlined.WbSunny, "天氣資訊")
            Spacer(Modifier.height(8.dp))
            InfoLink("交通部中央氣象署", "https://www.cwa.gov.tw/V8/C/")
        }

        InfoCard {
            // ▼▼▼ 新增：標題帶黃色圖示 ▼▼▼
            YellowIconTitle(Icons.Outlined.Cloud, "空氣資訊")
            Spacer(Modifier.height(8.dp))
            InfoLink("環境部環境保護署", "https://www.ema.gov.tw/")
        }

        InfoCard {
            // ▼▼▼ 新增：標題帶黃色圖示 ▼▼▼
            YellowIconTitle(Icons.Outlined.EmojiEvents, "成績與賽事資訊")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoLink("中華民國田徑協會", "https://www.athletics.org.tw/")
                InfoLink("屏東縣田徑委員會", "http://ptctf.ptc.edu.tw/")
                InfoLink("高雄市政府運動發展局", "https://sports.kcg.gov.tw/")
                InfoLink("臺南市政府體育局", "https://sports.tainan.gov.tw/html2/main")
                InfoLink("桃城體育網", "https://www.cysport.tw")
                InfoLink("嘉義縣CMS管理資訊系統", "https://cms.cyc.edu.tw")
                InfoLink("彰化縣體育會田徑委員會", "https://w69.tcctf.idv.tw/changhua2025/score_query.php?action=menu&htm=2&mode=1&ItNo=21&debug=1")
                InfoLink("彰化市體育會", "https://mkez.tw/500/")
                InfoLink("苗栗縣田徑委員會", "https://sites.google.com/jnes.mlc.edu.tw/mtfc")
                InfoLink("桃園市體育總會競賽資訊網", "http://sports.taoyuansport.org.tw/Default.htm")
                InfoLink("臺北市體育總會田徑協會", "https://www.tmtfa.com.tw/")
                InfoLink("新北市體育資訊網路中心", "https://ntpc-sports.com/")
                InfoLink("臺東縣體育會", "https://sites.google.com/site/ttsf2015/")
                InfoLink("花蓮體育網", "https://sport.hlc.edu.tw/hlc_city114/Module/Home/Index.php")
            }
        }
    }
}

// ─────────────────────────────────────────────
// 子頁 3：問題回報
// ─────────────────────────────────────────────

@Composable
fun FeedbackScreen(nav: NavHostController) {
    InfoScaffold(title = "問題回報", nav = nav) {
        InfoCard(title = "回報方式") {
            Text(
                text = "若您在使用過程中遇到錯誤、功能異常或有任何建議，歡迎來信與我們聯繫。",
                fontSize = 14.sp,
                color = BrandTextMain,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_email),
                    contentDescription = null,
                    tint = BrandYellow, // 信封維持黃色
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "XXX@gmail.com",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextMain
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 子頁 4：常見問題
// ─────────────────────────────────────────────

@Composable
fun FAQScreen(nav: NavHostController) {
    InfoScaffold(title = "常見問題", nav = nav) {
        // 這裡維持原本設計，Q是黃色就很跳了
        FaqCard(
            q = "為什麼比完賽之後「成績紀錄」的資料沒有更新？",
            a = "請先確認裝置網路是否正常連線，並點擊「歷史成績」右下角的同步按鈕。系統會自動抓取中華民國田徑協會所公布的最新成績。若該筆成績未登錄於田協，請使用「新增成績」功能手動補上。若同步後仍顯示舊資料，歡迎透過「問題回報」與我們聯繫。"
        )
        FaqCard(
            q = "我的個人資料安全嗎？",
            a = "本 APP 完整遵循隱私政策。所有使用者資料皆僅限於功能運作所需，不會外流、出售或提供給第三方。"
        )
        FaqCard(
            q = "影片分析一直顯示錯誤，該怎麼解決？",
            a = "請確認影片格式為 mp4 或 mov，並避免檔案過大。若是在雲端取得影片，請先下載到本機後再上傳。如問題持續，請使用「問題回報」功能。"
        )
        FaqCard(
            q = "為什麼影片分析要等一段時間？",
            a = "APP 使用影像辨識模型進行逐影格分析，需要一定的運算時間。分析期間請保持網路穩定，並避免切換離開 APP。"
        )
        FaqCard(
            q = "跑步影片分析的準確度如何？",
            a = "模型是依據大量跑步姿勢資料訓練而成，可提供高品質的關鍵節點與步態事件（著地、中間站立、離地）辨識。若拍攝角度偏差、光線不足，可能會影響分析效果。"
        )
        FaqCard(
            q = "若我拍攝的影片角度不太好，系統還能分析嗎？",
            a = "請使用側拍視角、保持鏡頭穩定、全身需入鏡。若條件不足，分析結果可能降低，但仍會盡力提供可用資訊。"
        )
        FaqCard(
            q = "更換手機後，我的資料會遺失嗎？",
            a = "不會。所有資料都綁定於您的帳號，登入後即可自動同步。"
        )
        FaqCard(
            q = "為什麼我看不到我的賽事成績？",
            a = "可能原因包括：\n．田協尚未公布成績\n．賽事不屬於田協認可項目\n．資料登錄延遲\n\n如需協助，請透過問題回報與我們聯繫。"
        )
    }
}

// ─────────────────────────────────────────────
// 子頁 5：服務條款 (小標題改黃色)
// ─────────────────────────────────────────────

@Composable
fun TermsScreen(nav: NavHostController) {
    InfoScaffold(title = "服務條款", nav = nav) {
        InfoCard(title = "歡迎使用") {
            Text(
                text = "歡迎使用本 APP（以下稱「本服務」）。為保障您的權益，請您在使用本服務前，務必詳細閱讀並同意以下條款。當您開始使用本服務，即表示您已閱讀、瞭解並同意遵守本條款之所有內容。",
                fontSize = 14.sp,
                color = BrandTextMain,
                lineHeight = 22.sp
            )
        }

        TermCard(
            heading = "一、服務內容",
            lines = listOf(
                "賽事成績查詢服務：串接中華民國田徑協會及各縣市相關協會公開資料，提供使用者查詢各級賽事之成績資訊。",
                "即時天氣資訊：透過交通部中央氣象署及環境部環境保護署開放資料 API，提供天氣現況、氣溫、降雨機率與空氣品質（AQI）。",
                "跑姿分析（影像辨識）：運用 YOLO 與 MediaPipe 進行跑步三事件辨識（著地、中間站立、離地）、關節角度計算與分析後影片覆疊。",
                "成績紀錄服務：提供個人分析紀錄保存與成績同步管理。",
                "開發團隊保留隨時新增、修改或移除功能的權利。"
            )
        )

        TermCard(
            heading = "二、使用者義務",
            lines = listOf(
                "遵守中華民國法律及相關國際規範。",
                "不得侵害他人之智慧財產權、隱私權、名譽或其他權利。",
                "不得未經授權存取、修改、影響或破壞本服務資料與系統。",
                "不得使用本服務執行詐欺、濫用、惡意程式散布或其他不法行為。",
                "若違反條款，開發團隊得暫停或終止使用資格，並依法追究責任。"
            )
        )

        TermCard(
            heading = "三、資料正確性與免責聲明",
            lines = listOf(
                "本服務之比賽、健康、天氣與空氣品質資訊皆來自政府公開資料或公開 API。",
                "因資料來源更新時間、網路狀態或系統異常等因素，資訊可能出現延遲、缺漏或錯誤，本服務不負擔任何賠償責任。",
                "使用者應自行判斷資料之適用性並承擔使用風險，本服務僅作為資訊參考與運動管理輔助工具。"
            )
        )

        TermCard(
            heading = "四、智慧財產權",
            lines = listOf(
                "除另有標示外，本服務內所有文字、圖片、設計、圖表、程式碼及相關內容皆為開發團隊所有，受著作權及智慧財產權法規保護。",
                "未經書面授權，不得擅自複製、散布、修改、公開展示或進行商業用途。"
            )
        )

        TermCard(
            heading = "五、服務變更與中斷",
            lines = listOf(
                "因系統維護或更新、伺服器或網路故障、資料來源單位中止提供資訊、天災或不可抗力等情形，開發團隊得調整或中止部分或全部服務，無須負擔任何賠償責任。"
            )
        )

        TermCard(
            heading = "六、隱私保護",
            lines = listOf(
                "本服務將依據《隱私政策》蒐集、使用與保護使用者個人資料。",
                "使用者同意在必要範圍內提供個人資訊以提供服務。"
            )
        )

        TermCard(
            heading = "七、條款修改",
            lines = listOf(
                "開發團隊保留隨時修訂本條款之權利，修改後將公告於本服務中。",
                "若使用者於更新後仍繼續使用，即視為同意修訂內容。"
            )
        )

        TermCard(
            heading = "八、準據法與管轄",
            lines = listOf(
                "本條款以中華民國法律為準據法。",
                "如因本服務發生爭議，雙方同意以臺灣臺北地方法院為第一審管轄法院。"
            )
        )
    }
}

// ─────────────────────────────────────────────
// 子頁 6：隱私政策 (小標題改黃色)
// ─────────────────────────────────────────────

@Composable
fun PrivacyPolicyScreen(nav: NavHostController) {
    InfoScaffold(title = "隱私政策", nav = nav) {
        InfoCard(title = "前言") {
            Text(
                text = "本 APP（以下稱「本服務」）重視使用者隱私，依據《個人資料保護法》與相關規範制定本政策，說明個人資料之蒐集、使用與保護方式。",
                fontSize = 14.sp,
                color = BrandTextMain,
                lineHeight = 22.sp
            )
        }

        TermCard(
            heading = "一、資料蒐集範圍",
            lines = listOf(
                "使用者提供資料：姓名、Email、帳號資訊（若有註冊）、問題回報內容或使用者自行上傳之資料。",
                "系統自動蒐集資料：裝置型號、系統版本、使用時間、APP 操作紀錄與非個資型統計資料。",
                "第三方 API 資料：中華民國田徑協會成績，及氣象與空氣品質相關開放資料。"
            )
        )

        TermCard(
            heading = "二、資料使用目的",
            lines = listOf(
                "提供成績查詢、賽事資訊與個人運動紀錄管理功能。",
                "改善服務體驗並進行系統功能優化。",
                "回覆問題回報並提供使用者支援。",
                "用於內部統計與研究，不涉及個人識別。"
            )
        )

        TermCard(
            heading = "三、資料保存與安全",
            lines = listOf(
                "所有個人資料均以加密方式儲存於安全伺服器。",
                "採取必要技術與管理措施避免外洩、竊取或濫用。",
                "非經使用者同意或法律要求，不會出售、交換或提供資料予第三方。",
                "如發生資料外洩事件，將立即通知使用者並採取補救措施。"
            )
        )

        TermCard(
            heading = "四、使用者權利",
            lines = listOf(
                "使用者得依相關法規請求查詢、閱覽、取得複本、補充或更正個人資料。",
                "並可請求停止蒐集、處理、利用或刪除個人資料（依法本服務得保留必要紀錄）。",
                "申請方式：可透過「聯絡我們」頁面或 Email：xxx@gmail.com 與我們聯繫。"
            )
        )

        TermCard(
            heading = "五、隱私政策修改",
            lines = listOf(
                "本政策如有修改，將公告於本服務內。",
                "修改後若繼續使用，即視為同意更新後內容。"
            )
        )
    }
}

// ─────────────────────────────────────────────
// 子頁 7：連絡我們
// ─────────────────────────────────────────────

@Composable
fun ContactScreen(nav: NavHostController) {
    InfoScaffold(title = "連絡我們", nav = nav) {
        InfoCard(title = "聯絡資訊") {
            Text(
                text = "感謝您使用本 APP，如有任何疑問、建議或問題回報，歡迎透過以下方式與我們聯繫：",
                fontSize = 14.sp,
                color = BrandTextMain,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_email),
                    contentDescription = null,
                    tint = BrandYellow, // 信封維持黃色
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Email 信箱",
                        fontSize = 14.sp,
                        color = BrandTextSecondary
                    )
                    Text(
                        text = "xxx@gmail.com",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextMain
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "我們將於 2–3 個工作日內回覆您的來信。",
                fontSize = 14.sp,
                color = BrandTextSecondary
            )
        }
    }
}

// ─────────────────────────────────────────────
// 共用排版元件
// ─────────────────────────────────────────────

// 子頁面的通用框架
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoScaffold(
    title: String,
    nav: NavHostController,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandTextMain
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = "Back",
                            modifier = Modifier.rotate(180f),
                            tint = BrandTextMain // 返回鍵保持深色
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BrandSurface
                )
            )
        },
        containerColor = BrandSurface
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content
                )
            }
        }
    }
}

// 通用的資訊卡片
@Composable
private fun InfoCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = BrandSurfaceVariant,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp), // 增加內部 padding 讓畫面更寬敞大氣
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextMain
                )
                Spacer(Modifier.height(4.dp))
            }
            content()
        }
    }
}

// 資料來源連結
@Composable
private fun InfoLink(name: String, url: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .padding(vertical = 8.dp, horizontal = 4.dp) // 增加點擊區域與間距
    ) {
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = BrandTextMain
        )
        Text(
            text = url,
            fontSize = 13.sp,
            color = Color(0xFF1565C0),
            lineHeight = 18.sp
        )
    }
}

// 常見問題卡片
@Composable
private fun FaqCard(q: String, a: String) {
    InfoCard {
        Row(verticalAlignment = Alignment.Top) {
            Text("Q：", fontWeight = FontWeight.Bold, color = BrandYellow, fontSize = 16.sp) // Q 稍微大一點
            Text(q, fontWeight = FontWeight.Bold, color = BrandTextMain, fontSize = 15.sp, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text("A：", fontWeight = FontWeight.Bold, color = BrandTextSecondary, fontSize = 16.sp)
            Text(a, fontSize = 14.sp, color = BrandTextMain, lineHeight = 22.sp)
        }
    }
}

// 條款列表卡片 (修改：小標題改為黃色)
@Composable
private fun TermCard(
    heading: String,
    lines: List<String>
) {
    // 這裡不傳 title 給 InfoCard，而是自己畫一個黃色的標題
    InfoCard {
        Text(
            text = heading,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = BrandYellow, // ▼▼▼ 改成黃色 ▼▼▼
            modifier = Modifier.padding(bottom = 8.dp)
        )
        lines.forEach { line ->
            Row(
                modifier = Modifier.padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("．", color = BrandYellow, fontWeight = FontWeight.Bold) // 圓點維持黃色
                Text(
                    text = line,
                    fontSize = 14.sp,
                    color = BrandTextMain,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// 輔助元件：帶黃色Icon的標題
@Composable
private fun YellowIconTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandYellow,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = BrandTextMain
        )
    }
}