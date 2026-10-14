/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class IconCategory(val label: String) {
    SOCIAL("Social"),
    MEDIA("Media"),
    TOOLS("Tools"),
    FINANCE("Finance"),
    GAMES("Games"),
    TRAVEL("Travel"),
    SHOPPING("Shopping"),
    OFFICE("Office"),
    LIFE("Lifestyle"),
}

data class CatalogIcon(
    val id: String,
    val name: String,
    val category: IconCategory,
    val vector: ImageVector,
)

/**
 * The built-in icon collection: Material icons from the compose material-icons-extended
 * library (Apache 2.0). One list feeds the grid, the search and the lookup by saved id.
 */
object IconCatalog {
    val all: List<CatalogIcon> = buildList {
        fun add(id: String, name: String, category: IconCategory, vector: ImageVector) {
            add(CatalogIcon(id, name, category, vector))
        }
        val s = IconCategory.SOCIAL
        add("chat", "Chat", s, Icons.Filled.Chat)
        add("forum", "Forum", s, Icons.Filled.Forum)
        add("mail", "Mail", s, Icons.Filled.Mail)
        add("call", "Call", s, Icons.Filled.Call)
        add("group", "Group", s, Icons.Filled.Group)
        add("person", "Person", s, Icons.Filled.Person)
        add("share", "Share", s, Icons.Filled.Share)
        add("thumb_up", "Thumbs up", s, Icons.Filled.ThumbUp)
        add("favorite", "Heart", s, Icons.Filled.Favorite)
        add("public", "Globe", s, Icons.Filled.Public)
        add("language", "Language", s, Icons.Filled.Language)
        add("notifications", "Bell", s, Icons.Filled.Notifications)
        add("emoji", "Smile", s, Icons.Filled.EmojiEmotions)
        add("cake", "Cake", s, Icons.Filled.Cake)

        val m = IconCategory.MEDIA
        add("music_note", "Music note", m, Icons.Filled.MusicNote)
        add("headphones", "Headphones", m, Icons.Filled.Headphones)
        add("videocam", "Video camera", m, Icons.Filled.Videocam)
        add("movie", "Movie", m, Icons.Filled.Movie)
        add("photo", "Photo", m, Icons.Filled.Photo)
        add("photo_camera", "Camera", m, Icons.Filled.PhotoCamera)
        add("mic", "Microphone", m, Icons.Filled.Mic)
        add("play", "Play", m, Icons.Filled.PlayArrow)
        add("radio", "Radio", m, Icons.Filled.Radio)
        add("podcasts", "Podcasts", m, Icons.Filled.Podcasts)
        add("library_music", "Music library", m, Icons.Filled.LibraryMusic)
        add("tv", "TV", m, Icons.Filled.Tv)
        add("album", "Album", m, Icons.Filled.Album)
        add("image", "Image", m, Icons.Filled.Image)

        val t = IconCategory.TOOLS
        add("settings", "Settings", t, Icons.Filled.Settings)
        add("build", "Wrench", t, Icons.Filled.Build)
        add("search", "Search", t, Icons.Filled.Search)
        add("lock", "Lock", t, Icons.Filled.Lock)
        add("key", "Key", t, Icons.Filled.Key)
        add("cloud", "Cloud", t, Icons.Filled.Cloud)
        add("download", "Download", t, Icons.Filled.Download)
        add("folder", "Folder", t, Icons.Filled.Folder)
        add("code", "Code", t, Icons.Filled.Code)
        add("terminal", "Terminal", t, Icons.Filled.Terminal)
        add("wifi", "Wi-Fi", t, Icons.Filled.Wifi)
        add("bluetooth", "Bluetooth", t, Icons.Filled.Bluetooth)
        add("battery", "Battery", t, Icons.Filled.BatteryFull)
        add("flashlight", "Flashlight", t, Icons.Filled.FlashlightOn)
        add("qr_code", "QR code", t, Icons.Filled.QrCode)
        add("security", "Shield", t, Icons.Filled.Security)
        add("extension", "Puzzle", t, Icons.Filled.Extension)
        add("storage", "Storage", t, Icons.Filled.Storage)
        add("memory", "Chip", t, Icons.Filled.Memory)

        val f = IconCategory.FINANCE
        add("bank", "Bank", f, Icons.Filled.AccountBalance)
        add("money", "Money", f, Icons.Filled.AttachMoney)
        add("credit_card", "Credit card", f, Icons.Filled.CreditCard)
        add("savings", "Savings", f, Icons.Filled.Savings)
        add("wallet", "Wallet", f, Icons.Filled.AccountBalanceWallet)
        add("trending_up", "Trending up", f, Icons.Filled.TrendingUp)
        add("pie_chart", "Pie chart", f, Icons.Filled.PieChart)
        add("bar_chart", "Bar chart", f, Icons.Filled.BarChart)
        add("receipt", "Receipt", f, Icons.Filled.Receipt)
        add("paid", "Coin", f, Icons.Filled.Paid)

        val g = IconCategory.GAMES
        add("esports", "Gamepad", g, Icons.Filled.SportsEsports)
        add("soccer", "Soccer", g, Icons.Filled.SportsSoccer)
        add("basketball", "Basketball", g, Icons.Filled.SportsBasketball)
        add("casino", "Dice", g, Icons.Filled.Casino)
        add("trophy", "Trophy", g, Icons.Filled.EmojiEvents)
        add("toys", "Toys", g, Icons.Filled.Toys)
        add("rocket", "Rocket", g, Icons.Filled.RocketLaunch)
        add("bolt", "Bolt", g, Icons.Filled.Bolt)
        add("psychology", "Brain", g, Icons.Filled.Psychology)
        add("style", "Cards", g, Icons.Filled.Style)

        val tr = IconCategory.TRAVEL
        add("flight", "Plane", tr, Icons.Filled.Flight)
        add("car", "Car", tr, Icons.Filled.DirectionsCar)
        add("bus", "Bus", tr, Icons.Filled.DirectionsBus)
        add("train", "Train", tr, Icons.Filled.Train)
        add("map", "Map", tr, Icons.Filled.Map)
        add("place", "Pin", tr, Icons.Filled.Place)
        add("hotel", "Hotel", tr, Icons.Filled.Hotel)
        add("explore", "Compass", tr, Icons.Filled.Explore)
        add("luggage", "Luggage", tr, Icons.Filled.Luggage)
        add("two_wheeler", "Scooter", tr, Icons.Filled.TwoWheeler)
        add("navigation", "Navigation", tr, Icons.Filled.Navigation)
        add("terrain", "Mountain", tr, Icons.Filled.Terrain)

        val sh = IconCategory.SHOPPING
        add("cart", "Cart", sh, Icons.Filled.ShoppingCart)
        add("bag", "Bag", sh, Icons.Filled.ShoppingBag)
        add("store", "Store", sh, Icons.Filled.Store)
        add("offer", "Tag", sh, Icons.Filled.LocalOffer)
        add("mall", "Mall", sh, Icons.Filled.LocalMall)
        add("sell", "Sell", sh, Icons.Filled.Sell)
        add("redeem", "Gift", sh, Icons.Filled.Redeem)
        add("storefront", "Storefront", sh, Icons.Filled.Storefront)

        val o = IconCategory.OFFICE
        add("document", "Document", o, Icons.Filled.Description)
        add("work", "Briefcase", o, Icons.Filled.Work)
        add("school", "School", o, Icons.Filled.School)
        add("book", "Book", o, Icons.Filled.Book)
        add("edit", "Pencil", o, Icons.Filled.Edit)
        add("calculate", "Calculator", o, Icons.Filled.Calculate)
        add("calendar", "Calendar", o, Icons.Filled.CalendarMonth)
        add("alarm", "Alarm", o, Icons.Filled.Alarm)
        add("schedule", "Clock", o, Icons.Filled.Schedule)
        add("assignment", "Clipboard", o, Icons.Filled.Assignment)
        add("print", "Printer", o, Icons.Filled.Print)
        add("article", "Article", o, Icons.Filled.Article)
        add("task", "Task done", o, Icons.Filled.TaskAlt)

        val l = IconCategory.LIFE
        add("home", "Home", l, Icons.Filled.Home)
        add("star", "Star", l, Icons.Filled.Star)
        add("restaurant", "Restaurant", l, Icons.Filled.Restaurant)
        add("cafe", "Coffee", l, Icons.Filled.LocalCafe)
        add("fitness", "Fitness", l, Icons.Filled.FitnessCenter)
        add("pets", "Paw", l, Icons.Filled.Pets)
        add("spa", "Spa", l, Icons.Filled.Spa)
        add("eco", "Leaf", l, Icons.Filled.Eco)
        add("sunny", "Sun", l, Icons.Filled.WbSunny)
        add("night", "Moon", l, Icons.Filled.NightsStay)
        add("lightbulb", "Light bulb", l, Icons.Filled.Lightbulb)
        add("palette", "Palette", l, Icons.Filled.Palette)
        add("brush", "Brush", l, Icons.Filled.Brush)
        add("hospital", "Hospital", l, Icons.Filled.LocalHospital)
    }

    private val byId: Map<String, CatalogIcon> = all.associateBy { it.id }

    fun find(id: String): CatalogIcon? = byId[id]

    /** Icons whose name or id contains [query], limited to [category] when given. */
    fun search(query: String, category: IconCategory?): List<CatalogIcon> {
        val q = query.trim().lowercase()
        return all.filter { icon ->
            (category == null || icon.category == category) &&
                (q.isEmpty() || icon.name.lowercase().contains(q) || icon.id.contains(q))
        }
    }

    /** Background colours offered in the editor, as ARGB ints. */
    val palette: List<Color> = listOf(
        0xFF3949AB, 0xFF1E88E5, 0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342,
        0xFFFDD835, 0xFFFB8C00, 0xFFF4511E, 0xFFE53935, 0xFFD81B60, 0xFF8E24AA,
        0xFF5E35B1, 0xFF6D4C41, 0xFF546E7A, 0xFF212121,
    ).map { Color(it) }
}
