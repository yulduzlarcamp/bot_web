package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * ASQAROVBULL BOT - Full Optimized Version
 * Telegram: @AsqarovBull_bot
 */
public class BulldropBotApp {

    // ============================================
    // KONSTANTALAR
    // ============================================
    private static final String BOT_USERNAME = "AsqarovBull_bot";
    private static final String BOT_TOKEN = "8594559352:AAHrL5V4dnC18yctL60SR-p83Uq1__64ynQ";
    private static final String MENU_IMAGE_PATH = "img.png";
    private static final Set<Long> SUPER_ADMINS = new HashSet<>(Arrays.asList(8462250622L));

    // ============================================
    // DATA STORAGE
    // ============================================
    private static final Map<Long, User> USERS = new ConcurrentHashMap<>();
    private static final Map<Long, AdminInfo> ADMINS = new ConcurrentHashMap<>();
    private static final Map<Long, Drop> DROPS = new ConcurrentHashMap<>();
    private static final Map<Long, Contest> CONTESTS = new ConcurrentHashMap<>();
    private static final Map<Long, AdminWizard> ADMIN_WIZ = new ConcurrentHashMap<>();
    private static final List<RequiredChannel> REQUIRED_CHANNELS = Collections.synchronizedList(new ArrayList<>());

    private static final AtomicLong LAST_DROP_ID = new AtomicLong(0);
    private static final AtomicLong LAST_CONTEST_ID = new AtomicLong(0);
    private static final Instant BOT_START_TIME = Instant.now();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final ReentrantLock dropLock = new ReentrantLock();

    private static InputFile cachedImage = null;
    private static BotSettings botSettings = new BotSettings();
    private static boolean isRunning = true;

    static {
        // SUPER ADMINLAR
        for (Long adminId : SUPER_ADMINS) {
            ADMINS.put(adminId, new AdminInfo(adminId, "Super Admin", "", 0L, true));
        }

        // DEFAULT KANALLAR
        REQUIRED_CHANNELS.add(new RequiredChannel("@TOPMI_MEDIA", "https://t.me/TOPMI_MEDIA", "TOPMI MEDIA", true));
        REQUIRED_CHANNELS.add(new RequiredChannel("@ASKAROV1M", "https://t.me/ASKAROV1M", "ASKAROV 1M", true));
    }

    // ============================================
    // MAIN METHOD
    // ============================================
    public static void main(String[] args) {
        try {
            System.out.println("╔════════════════════════════════════╗");
            System.out.println("║    🚀 ASQAROVBULL BOT v2.0        ║");
            System.out.println("╚════════════════════════════════════╝");

            // Ulanish vaqtlarini oshirish
            System.setProperty("sun.net.client.defaultConnectTimeout", "60000");
            System.setProperty("sun.net.client.defaultReadTimeout", "60000");

            loadImage();
            initializeData();
            startScheduledTasks();

            // Botni ishga tushirish
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            BulldropBot bot = new BulldropBot();
            api.registerBot(bot);

            System.out.println("✅ Bot muvaffaqiyatli ishga tushdi!");
            System.out.println("📊 Bot ma'lumotlari:");
            System.out.println("   • Foydalanuvchilar: " + USERS.size());
            System.out.println("   • Adminlar: " + ADMINS.size());
            System.out.println("   • Kanallar: " + REQUIRED_CHANNELS.size());
            System.out.println("   • Droplar: " + DROPS.size());
            System.out.println("   • Konkurslar: " + CONTESTS.size());
            System.out.println("\n⏳ Bot ishlamoqda... (To'xtatish uchun Ctrl+C)");

            // JARAYONNI TO'XTATGUNCHA KUTISH
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Bot to'xtatilmoqda...");
                isRunning = false;
                scheduler.shutdown();
                try {
                    scheduler.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("✅ Bot to'xtatildi!");
            }));

            // Infinite loop
            while (isRunning) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.err.println("❌ Xatolik: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadImage() {
        try {
            // Rasmni turli joylardan qidirish
            String[] paths = {
                    MENU_IMAGE_PATH,
                    "src/main/resources/" + MENU_IMAGE_PATH,
                    "target/classes/" + MENU_IMAGE_PATH,
                    "/opt/bot/" + MENU_IMAGE_PATH
            };

            for (String path : paths) {
                File imageFile = new File(path);
                if (imageFile.exists()) {
                    cachedImage = new InputFile(imageFile, "menu.png");
                    System.out.println("✅ Rasm yuklandi: " + path);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Rasm yuklanmadi: " + e.getMessage());
        }
    }

    private static void initializeData() {
        // Test drop
        if (DROPS.isEmpty()) {
            Drop drop = new Drop(LAST_DROP_ID.incrementAndGet());
            drop.name = "Welcome Drop";
            drop.description = "Birinchi drop! 2 ta do'st taklif qiling";
            drop.requiredRefs = 2;
            drop.createdBy = SUPER_ADMINS.iterator().next();
            drop.active = true;
            drop.codes.addAll(Arrays.asList("WELCOME123", "BONUS456", "SECRET789"));
            drop.totalCodes = drop.codes.size();
            DROPS.put(drop.id, drop);
        }

        // Test contest
        if (CONTESTS.isEmpty()) {
            Contest contest = new Contest(LAST_CONTEST_ID.incrementAndGet(), SUPER_ADMINS.iterator().next());
            contest.name = "Birinchi Konkurs";
            contest.description = "3 ta do'st taklif qiling";
            contest.prize = "1000 so'm bonus";
            contest.winnerCount = 3;
            contest.requiredRefs = 3;
            contest.startDate = Instant.now();
            contest.endDate = Instant.now().plusSeconds(7 * 24 * 60 * 60);
            contest.active = true;
            CONTESTS.put(contest.id, contest);
        }
    }

    private static void startScheduledTasks() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                BulldropBot.checkFinishedContests();
            } catch (Exception e) {
                System.err.println("❌ Scheduler xatolik: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.HOURS);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                BulldropBot.resetDailyLimits();
            } catch (Exception e) {
                System.err.println("❌ Reset xatolik: " + e.getMessage());
            }
        }, 0, 24, TimeUnit.HOURS);
    }

    // ============================================
    // DATA CLASSES
    // ============================================

    private static class RequiredChannel {
        String username, link, name;
        boolean active;
        RequiredChannel(String username, String link, String name, boolean active) {
            this.username = username; this.link = link; this.name = name; this.active = active;
        }
    }

    private static class BotSettings {
        String language = "uz";
        String welcomeMessage = "👋 Xush kelibsiz!";
        String joinMessage = "❗ Botdan foydalanish uchun kanallarga azo bo'ling!";
        boolean requireJoin = true;
        boolean allowReferrals = true;
        int maxDailyClaims = 10;
        String botName = "ASQAROVBULL BOT";
        boolean antiSpam = true;
        int spamTimeSeconds = 30;
    }

    private static class AdminInfo {
        final long id; final String name; final String username;
        final long addedBy; final Instant addedAt; final boolean isSuperAdmin;
        AdminInfo(long id, String name, String username, long addedBy, boolean isSuperAdmin) {
            this.id = id; this.name = name; this.username = username;
            this.addedBy = addedBy; this.addedAt = Instant.now(); this.isSuperAdmin = isSuperAdmin;
        }
    }

    private static class User {
        final long tgId; String username; String firstName; String lastName;
        Instant createdAt = Instant.now(); Long referredBy = null;
        int refCount = 0; int totalCodesClaimed = 0; int totalContestsParticipated = 0;
        int totalWins = 0; boolean blocked = false; Instant lastClaimTime = null;
        int dailyClaims = 0; final Map<Long, Boolean> joinedContests = new ConcurrentHashMap<>();
        User(long tgId) { this.tgId = tgId; }
    }

    private static class Drop {
        final long id; String name = ""; String description = "";
        int requiredRefs = 0; boolean active = false;
        final long createdAt = System.currentTimeMillis(); long createdBy;
        final Deque<String> codes = new ArrayDeque<>();
        final Set<Long> claimedUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        int totalCodes = 0; int claimedCount = 0; Instant startTime = null; Instant endTime = null;
        int maxClaims = 0;
        Drop(long id) { this.id = id; }
    }

    private static class Contest {
        final long id; String name = ""; String description = ""; String prize = "";
        int winnerCount = 1; Instant startDate; Instant endDate;
        boolean active = false; boolean finished = false;
        final long createdBy; final Instant createdAt = Instant.now();
        final Set<Long> participants = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final List<Long> winners = new ArrayList<>(); int requiredRefs = 0;
        Contest(long id, long createdBy) { this.id = id; this.createdBy = createdBy; }
    }

    private enum AdminStage {
        NONE, WAIT_DROP_NAME, WAIT_DROP_DESCRIPTION, WAIT_REQUIRED_REFS, WAIT_CODES,
        WAIT_ADD_ADMIN, WAIT_REMOVE_ADMIN, WAIT_BROADCAST_MESSAGE, WAIT_BROADCAST_PHOTO,
        WAIT_CONTEST_NAME, WAIT_CONTEST_DESCRIPTION, WAIT_CONTEST_PRIZE, WAIT_CONTEST_WINNERS,
        WAIT_CONTEST_START, WAIT_CONTEST_END, WAIT_CONTEST_REFS, WAIT_CHANNEL_ADD, WAIT_CHANNEL_REMOVE
    }

    private static class AdminWizard {
        AdminStage stage = AdminStage.NONE;
        Drop building = null; Contest buildingContest = null;
        String broadcastMessage = null; InputFile broadcastPhoto = null;
        final long chatId; final int messageId;
        AdminWizard(long chatId, int messageId) { this.chatId = chatId; this.messageId = messageId; }
    }

    // ============================================
    // BOT CLASS
    // ============================================

    public static class BulldropBot extends TelegramLongPollingBot {

        private static final int MAX_RETRIES = 3;
        private static final int RETRY_DELAY = 5000;

        @Override
        public String getBotUsername() { return BOT_USERNAME; }
        @Override
        public String getBotToken() { return BOT_TOKEN; }

        @Override
        public void onUpdateReceived(Update update) {
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    if (update.hasMessage()) {
                        handleMessage(update.getMessage());
                    } else if (update.hasCallbackQuery()) {
                        handleCallback(update.getCallbackQuery());
                    }
                    break;
                } catch (Exception e) {
                    System.err.println("⚠️ Urinish " + attempt + "/" + MAX_RETRIES + " xatolik: " + e.getMessage());
                    if (attempt == MAX_RETRIES) {
                        System.err.println("❌ Barcha urinishlar muvaffaqiyatsiz");
                    } else {
                        try { Thread.sleep(RETRY_DELAY); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                }
            }
        }

        // ============================================
        // HELPER METHODS
        // ============================================

        private void sendMessage(long chatId, String text, InlineKeyboardMarkup kb) {
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(text);
                message.setParseMode("HTML");
                message.setDisableWebPagePreview(true);
                if (kb != null) message.setReplyMarkup(kb);
                execute(message);
            } catch (TelegramApiException e) {
                System.err.println("❌ Xabar yuborilmadi: " + e.getMessage());
            }
        }

        private void sendPhoto(long chatId, InputFile photo, String caption, InlineKeyboardMarkup kb) {
            try {
                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(chatId);
                sendPhoto.setPhoto(photo);
                sendPhoto.setCaption(caption);
                sendPhoto.setParseMode("HTML");
                if (kb != null) sendPhoto.setReplyMarkup(kb);
                execute(sendPhoto);
            } catch (TelegramApiException e) {
                sendMessage(chatId, caption + "\n(Rasm yuborilmadi)", kb);
            }
        }

        private void editMessage(long chatId, int msgId, String text, InlineKeyboardMarkup kb, boolean hasPhoto) {
            try {
                if (hasPhoto) {
                    EditMessageCaption edit = new EditMessageCaption();
                    edit.setChatId(chatId);
                    edit.setMessageId(msgId);
                    edit.setCaption(text);
                    edit.setParseMode("HTML");
                    if (kb != null) edit.setReplyMarkup(kb);
                    execute(edit);
                } else {
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId(chatId);
                    edit.setMessageId(msgId);
                    edit.setText(text);
                    edit.setParseMode("HTML");
                    edit.setDisableWebPagePreview(true);
                    if (kb != null) edit.setReplyMarkup(kb);
                    execute(edit);
                }
            } catch (TelegramApiException e) {
                if (!e.getMessage().contains("message is not modified")) {
                    System.err.println("❌ Edit xatolik: " + e.getMessage());
                }
            }
        }

        private void answerCallback(String callbackId, String text, boolean alert) {
            try {
                AnswerCallbackQuery answer = new AnswerCallbackQuery();
                answer.setCallbackQueryId(callbackId);
                if (text != null) {
                    answer.setText(text);
                    answer.setShowAlert(alert);
                }
                execute(answer);
            } catch (Exception ignored) {}
        }

        private InlineKeyboardButton createButton(String text, String data) {
            return InlineKeyboardButton.builder().text(text).callbackData(data).build();
        }

        private InlineKeyboardMarkup createKeyboard(List<List<InlineKeyboardButton>> rows) {
            return InlineKeyboardMarkup.builder().keyboard(rows).build();
        }

        // ============================================
        // CHANNEL CHECK
        // ============================================

        private boolean isJoinedAllChannels(long userId) {
            if (!botSettings.requireJoin) return true;
            try {
                for (RequiredChannel channel : REQUIRED_CHANNELS) {
                    if (!channel.active) continue;
                    GetChatMember request = new GetChatMember();
                    request.setChatId(channel.username);
                    request.setUserId(userId);
                    ChatMember member = execute(request);
                    String status = member.getStatus();
                    if (!("member".equals(status) || "administrator".equals(status) || "creator".equals(status))) {
                        return false;
                    }
                }
                return true;
            } catch (TelegramApiException e) {
                return false;
            }
        }

        private List<RequiredChannel> getNotJoinedChannels(long userId) {
            List<RequiredChannel> notJoined = new ArrayList<>();
            if (!botSettings.requireJoin) return notJoined;
            try {
                for (RequiredChannel channel : REQUIRED_CHANNELS) {
                    if (!channel.active) continue;
                    GetChatMember request = new GetChatMember();
                    request.setChatId(channel.username);
                    request.setUserId(userId);
                    ChatMember member = execute(request);
                    String status = member.getStatus();
                    if (!("member".equals(status) || "administrator".equals(status) || "creator".equals(status))) {
                        notJoined.add(channel);
                    }
                }
            } catch (TelegramApiException ignored) {}
            return notJoined;
        }

        // ============================================
        // ADMIN CHECK
        // ============================================

        private boolean isAdmin(long userId) {
            return ADMINS.containsKey(userId);
        }

        private boolean isSuperAdmin(long userId) {
            AdminInfo admin = ADMINS.get(userId);
            return admin != null && admin.isSuperAdmin;
        }

        // ============================================
        // DROP METHODS
        // ============================================

        private Drop getActiveDrop() {
            return DROPS.values().stream().filter(d -> d.active).findFirst().orElse(null);
        }

        // ============================================
        // CONTEST METHODS
        // ============================================

        private List<Contest> getActiveContests() {
            Instant now = Instant.now();
            return CONTESTS.values().stream()
                    .filter(c -> c.active && !c.finished)
                    .filter(c -> c.startDate == null || !now.isBefore(c.startDate))
                    .filter(c -> c.endDate == null || !now.isAfter(c.endDate))
                    .collect(Collectors.toList());
        }

        private static void checkFinishedContests() {
            Instant now = Instant.now();
            for (Contest c : CONTESTS.values()) {
                if (c.active && !c.finished && c.endDate != null && now.isAfter(c.endDate)) {
                    finishContest(c.id);
                }
            }
        }

        private static void finishContest(long contestId) {
            Contest contest = CONTESTS.get(contestId);
            if (contest == null || contest.finished) return;

            contest.active = false;
            contest.finished = true;

            if (!contest.participants.isEmpty()) {
                List<Long> participants = new ArrayList<>(contest.participants);
                Collections.shuffle(participants);
                int winnersToSelect = Math.min(contest.winnerCount, participants.size());
                for (int i = 0; i < winnersToSelect; i++) {
                    contest.winners.add(participants.get(i));
                }
            }
        }

        static void resetDailyLimits() {
            for (User user : USERS.values()) {
                user.dailyClaims = 0;
            }
        }

        // ============================================
        // MESSAGE HANDLER
        // ============================================

        private void handleMessage(Message message) {
            long chatId = message.getChatId();
            long userId = message.getFrom().getId();
            String text = message.hasText() ? message.getText().trim() : "";

            USERS.computeIfAbsent(userId, k -> {
                User u = new User(userId);
                u.username = message.getFrom().getUserName();
                u.firstName = message.getFrom().getFirstName();
                u.lastName = message.getFrom().getLastName();
                return u;
            });

            if (botSettings.antiSpam && !isAdmin(userId)) {
                User user = USERS.get(userId);
                if (user.lastClaimTime != null) {
                    long secondsSince = Instant.now().getEpochSecond() - user.lastClaimTime.getEpochSecond();
                    if (secondsSince < botSettings.spamTimeSeconds) {
                        sendMessage(chatId, "⏳ " + (botSettings.spamTimeSeconds - secondsSince) + " soniya kuting!", null);
                        return;
                    }
                }
                user.lastClaimTime = Instant.now();
            }

            if (isAdmin(userId) && handleAdminWizard(message)) {
                return;
            }

            if (text.startsWith("/start")) {
                handleStartCommand(userId, text);
                if (!isJoinedAllChannels(userId)) {
                    sendJoinChannelsMessage(chatId);
                } else {
                    sendMainMenu(chatId);
                }
            } else if (text.equals("/help")) {
                sendHelpMessage(chatId);
            } else if (text.equals("/stats") && isAdmin(userId)) {
                sendStatsMessage(chatId);
            } else if (text.equals("/cancel")) {
                cancelWizard(userId, chatId);
            } else if (text.equals("/admin") && isAdmin(userId)) {
                sendAdminMenu(chatId);
            } else {
                if (!isJoinedAllChannels(userId)) {
                    sendJoinChannelsMessage(chatId);
                } else {
                    sendMainMenu(chatId);
                }
            }
        }

        private void handleStartCommand(long userId, String text) {
            String[] parts = text.split(" ");
            if (parts.length < 2) return;
            try {
                long referrerId = Long.parseLong(parts[1]);
                if (referrerId == userId) return;

                User newUser = USERS.get(userId);
                if (newUser == null || newUser.referredBy != null) return;

                User referrer = USERS.get(referrerId);
                if (referrer == null) return;

                newUser.referredBy = referrerId;
                referrer.refCount++;
                sendMessage(referrerId, "🎉 Yangi referral! Jami: " + referrer.refCount, null);
            } catch (NumberFormatException ignored) {}
        }

        // ============================================
        // MENU METHODS
        // ============================================

        private void sendMainMenu(long chatId) {
            String text = "📦 <b>" + botSettings.botName + "</b>\n\n" +
                    "👋 " + botSettings.welcomeMessage + "\n\n" +
                    "Quyidagi tugmalardan birini tanlang:";

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(Arrays.asList(
                    createButton("🎁 Aktiv Drop", "menu_drop"),
                    createButton("👤 Profil", "menu_profile")
            ));

            List<Contest> activeContests = getActiveContests();
            if (!activeContests.isEmpty()) {
                rows.add(Arrays.asList(
                        createButton("🏆 Konkurslar (" + activeContests.size() + ")", "menu_contests")
                ));
            }

            rows.add(Arrays.asList(
                    createButton("🔗 Referral Link", "menu_ref"),
                    createButton("📊 Statistika", "menu_stats")
            ));

            rows.add(Arrays.asList(
                    createButton("🆘 Yordam", "menu_help")
            ));

            if (isAdmin(chatId)) {
                rows.add(Arrays.asList(
                        createButton("👑 Admin Panel", "menu_admin")
                ));
            }

            if (cachedImage != null) {
                sendPhoto(chatId, cachedImage, text, createKeyboard(rows));
            } else {
                sendMessage(chatId, text, createKeyboard(rows));
            }
        }

        private void sendJoinChannelsMessage(long chatId) {
            List<RequiredChannel> notJoined = getNotJoinedChannels(chatId);
            StringBuilder text = new StringBuilder("❗ <b>" + botSettings.joinMessage + "</b>\n\n");

            for (RequiredChannel channel : notJoined) {
                text.append("❌ ").append(channel.name).append(": ").append(channel.username).append("\n");
            }
            text.append("\n✅ Barcha kanallarga azo bo'lgach, 'Tekshirish' tugmasini bosing.");

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (RequiredChannel channel : notJoined) {
                rows.add(Arrays.asList(
                        InlineKeyboardButton.builder().text("📢 " + channel.name).url(channel.link).build()
                ));
            }
            rows.add(Arrays.asList(createButton("🔄 Tekshirish", "check_join")));

            if (cachedImage != null) {
                sendPhoto(chatId, cachedImage, text.toString(), createKeyboard(rows));
            } else {
                sendMessage(chatId, text.toString(), createKeyboard(rows));
            }
        }

        private void sendHelpMessage(long chatId) {
            String text = "🆘 <b>YORDAM</b>\n\n" +
                    "Komandalar:\n" +
                    "/start - Botni boshlash\n" +
                    "/help - Yordam oynasi\n" +
                    "/cancel - Jarayonni bekor qilish\n" +
                    "/stats - Statistika (adminlar uchun)\n" +
                    "/admin - Admin panel (adminlar uchun)";

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (RequiredChannel channel : REQUIRED_CHANNELS) {
                if (channel.active) {
                    rows.add(Arrays.asList(
                            InlineKeyboardButton.builder().text("📢 " + channel.name).url(channel.link).build()
                    ));
                }
            }
            rows.add(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")));
            sendMessage(chatId, text, createKeyboard(rows));
        }

        private void sendStatsMessage(long chatId) {
            long uptimeMs = System.currentTimeMillis() - BOT_START_TIME.toEpochMilli();
            long hours = uptimeMs / 3600000;
            long minutes = (uptimeMs / 60000) % 60;

            int totalRefs = USERS.values().stream().mapToInt(u -> u.refCount).sum();
            int totalCodes = DROPS.values().stream().mapToInt(d -> d.claimedCount).sum();
            int totalWinners = CONTESTS.values().stream().mapToInt(c -> c.winners.size()).sum();

            String text = "📊 <b>STATISTIKA</b>\n\n" +
                    "👥 Foydalanuvchilar: " + USERS.size() + "\n" +
                    "👑 Adminlar: " + ADMINS.size() + "\n" +
                    "🎁 Droplar: " + DROPS.size() + "\n" +
                    "🏆 Konkurslar: " + CONTESTS.size() + "\n" +
                    "🔑 Berilgan kodlar: " + totalCodes + "\n" +
                    "👥 Jami referallar: " + totalRefs + "\n" +
                    "🏅 G'oliblar: " + totalWinners + "\n" +
                    "⏱ Ish vaqti: " + hours + " soat " + minutes + " daqiqa";

            sendMessage(chatId, text, createKeyboard(Arrays.asList(
                    Arrays.asList(createButton("⬅️ Orqaga", "menu_main"))
            )));
        }

        private void cancelWizard(long userId, long chatId) {
            if (ADMIN_WIZ.remove(userId) != null) {
                sendMessage(chatId, "✅ Jarayon bekor qilindi.", null);
                sendMainMenu(chatId);
            } else {
                sendMessage(chatId, "❌ Faol jarayon topilmadi.", null);
            }
        }

        // ============================================
        // ADMIN WIZARD
        // ============================================

        private boolean handleAdminWizard(Message message) {
            long userId = message.getFrom().getId();
            AdminWizard wizard = ADMIN_WIZ.get(userId);
            if (wizard == null || wizard.stage == AdminStage.NONE) return false;

            long chatId = message.getChatId();
            String text = message.hasText() ? message.getText().trim() : "";
            boolean hasPhoto = message.hasPhoto();

            if (text.equals("/cancel")) {
                ADMIN_WIZ.remove(userId);
                sendMessage(chatId, "✅ Jarayon bekor qilindi.", null);
                sendMainMenu(chatId);
                return true;
            }

            switch (wizard.stage) {
                case WAIT_DROP_NAME:
                    if (text.isEmpty()) {
                        sendMessage(chatId, "Drop nomini kiriting:", null);
                        return true;
                    }
                    wizard.building.name = text;
                    wizard.stage = AdminStage.WAIT_DROP_DESCRIPTION;
                    sendMessage(chatId, "✅ Nomi saqlandi!\n\nIzoh yozing yoki /skip:", null);
                    return true;

                case WAIT_DROP_DESCRIPTION:
                    if (!text.equals("/skip")) {
                        wizard.building.description = text;
                    }
                    wizard.stage = AdminStage.WAIT_REQUIRED_REFS;
                    sendMessage(chatId, "Referral talab sonini kiriting:", null);
                    return true;

                case WAIT_REQUIRED_REFS:
                    try {
                        int refs = Integer.parseInt(text);
                        wizard.building.requiredRefs = Math.max(0, refs);
                        wizard.stage = AdminStage.WAIT_CODES;
                        sendMessage(chatId, "✅ Talab saqlandi!\n\nKodlarni birma-bir yozing.\nTugatish uchun /done:", null);
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "❌ Noto'g'ri son!", null);
                    }
                    return true;

                case WAIT_CODES:
                    if (text.equals("/done")) {
                        if (wizard.building.codes.isEmpty()) {
                            sendMessage(chatId, "❌ Kamida bitta kod kerak!", null);
                            return true;
                        }
                        wizard.building.totalCodes = wizard.building.codes.size();
                        wizard.building.active = true;
                        DROPS.values().forEach(d -> d.active = false);
                        DROPS.put(wizard.building.id, wizard.building);

                        sendMessage(chatId, "✅ <b>DROP YARATILDI!</b>\n\n" +
                                "📌 Nomi: " + wizard.building.name + "\n" +
                                "🔑 Kodlar soni: " + wizard.building.codes.size(), null);

                        ADMIN_WIZ.remove(userId);
                        sendMainMenu(chatId);
                        return true;
                    }

                    String[] lines = text.split("\n");
                    int added = 0;
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            wizard.building.codes.addLast(line.trim());
                            added++;
                        }
                    }
                    sendMessage(chatId, "✅ " + added + " ta kod qo'shildi.\nJami: " + wizard.building.codes.size() + "\n\nYana kod yozing yoki /done", null);
                    return true;

                case WAIT_ADD_ADMIN:
                    if (!isSuperAdmin(userId)) {
                        sendMessage(chatId, "❌ Faqat SUPER ADMIN admin qo'sha oladi!", null);
                        ADMIN_WIZ.remove(userId);
                        sendAdminMenu(chatId);
                        return true;
                    }
                    try {
                        long newAdminId = Long.parseLong(text);
                        if (ADMINS.containsKey(newAdminId)) {
                            sendMessage(chatId, "❌ Bu foydalanuvchi allaqachon admin!", null);
                        } else {
                            User user = USERS.get(newAdminId);
                            String name = user != null ? user.firstName : "Noma'lum";
                            String username = user != null ? user.username : "";
                            ADMINS.put(newAdminId, new AdminInfo(newAdminId, name, username, userId, false));
                            sendMessage(chatId, "✅ Admin qo'shildi: " + newAdminId, null);
                            sendMessage(newAdminId, "🎉 Siz admin qilindingiz! /admin orqali panelga kiring.", null);
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "❌ Noto'g'ri ID!", null);
                        return true;
                    }
                    ADMIN_WIZ.remove(userId);
                    sendAdminMenu(chatId);
                    return true;

                case WAIT_REMOVE_ADMIN:
                    if (!isSuperAdmin(userId)) {
                        sendMessage(chatId, "❌ Faqat SUPER ADMIN admin o'chira oladi!", null);
                        ADMIN_WIZ.remove(userId);
                        sendAdminMenu(chatId);
                        return true;
                    }
                    try {
                        long removeAdminId = Long.parseLong(text);
                        AdminInfo adminToRemove = ADMINS.get(removeAdminId);
                        if (adminToRemove == null) {
                            sendMessage(chatId, "❌ Bu ID admin emas!", null);
                        } else if (adminToRemove.isSuperAdmin) {
                            sendMessage(chatId, "❌ SUPER ADMINlarni o'chirib bo'lmaydi!", null);
                        } else {
                            ADMINS.remove(removeAdminId);
                            sendMessage(chatId, "✅ Admin o'chirildi: " + removeAdminId, null);
                            sendMessage(removeAdminId, "❌ Siz adminlikdan olib tashlandingiz.", null);
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "❌ Noto'g'ri ID!", null);
                        return true;
                    }
                    ADMIN_WIZ.remove(userId);
                    sendAdminMenu(chatId);
                    return true;

                case WAIT_BROADCAST_MESSAGE:
                    wizard.broadcastMessage = text;
                    wizard.stage = AdminStage.WAIT_BROADCAST_PHOTO;
                    sendMessage(chatId, "✅ Matn saqlandi!\n\nRasm yuboring yoki /skip:", null);
                    return true;

                case WAIT_BROADCAST_PHOTO:
                    if (hasPhoto) {
                        List<PhotoSize> photos = message.getPhoto();
                        PhotoSize largest = photos.stream().max(Comparator.comparing(PhotoSize::getFileSize)).orElse(null);
                        if (largest != null) wizard.broadcastPhoto = new InputFile(largest.getFileId());
                    }

                    int success = 0, failed = 0;
                    for (Long uid : USERS.keySet()) {
                        try {
                            if (wizard.broadcastPhoto != null) {
                                sendPhoto(uid, wizard.broadcastPhoto, wizard.broadcastMessage, null);
                            } else {
                                sendMessage(uid, wizard.broadcastMessage, null);
                            }
                            success++;
                        } catch (Exception e) {
                            failed++;
                        }
                        if (success % 30 == 0) {
                            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                        }
                    }

                    sendMessage(chatId, "✅ Broadcast yakunlandi!\nYuborildi: " + success + "\nYuborilmadi: " + failed, null);
                    ADMIN_WIZ.remove(userId);
                    sendAdminMenu(chatId);
                    return true;

                case WAIT_CONTEST_NAME:
                    wizard.buildingContest.name = text;
                    wizard.stage = AdminStage.WAIT_CONTEST_DESCRIPTION;
                    sendMessage(chatId, "✅ Nomi saqlandi!\n\nTavsif yozing:", null);
                    return true;

                case WAIT_CONTEST_DESCRIPTION:
                    wizard.buildingContest.description = text;
                    wizard.stage = AdminStage.WAIT_CONTEST_PRIZE;
                    sendMessage(chatId, "✅ Tavsif saqlandi!\n\nSovrin yozing:", null);
                    return true;

                case WAIT_CONTEST_PRIZE:
                    wizard.buildingContest.prize = text;
                    wizard.stage = AdminStage.WAIT_CONTEST_WINNERS;
                    sendMessage(chatId, "✅ Sovrin saqlandi!\n\nNechta g'olib bo'lsin?", null);
                    return true;

                case WAIT_CONTEST_WINNERS:
                    try {
                        int winners = Integer.parseInt(text);
                        wizard.buildingContest.winnerCount = Math.max(1, winners);
                        wizard.stage = AdminStage.WAIT_CONTEST_REFS;
                        sendMessage(chatId, "✅ G'oliblar soni saqlandi!\n\nReferral talab (0 - talab yo'q):", null);
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "❌ Noto'g'ri format!", null);
                    }
                    return true;

                case WAIT_CONTEST_REFS:
                    try {
                        int refs = Integer.parseInt(text);
                        wizard.buildingContest.requiredRefs = Math.max(0, refs);
                        wizard.stage = AdminStage.WAIT_CONTEST_START;
                        sendMessage(chatId, "✅ Referral talab saqlandi!\n\nBoshlanish vaqti (kun soni yoki /now):", null);
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "❌ Noto'g'ri format!", null);
                    }
                    return true;

                case WAIT_CONTEST_START:
                    if (text.equals("/now")) {
                        wizard.buildingContest.startDate = Instant.now();
                        wizard.stage = AdminStage.WAIT_CONTEST_END;
                        sendMessage(chatId, "✅ Hozir boshlanadi!\n\nTugash vaqti (kun soni):", null);
                    } else {
                        try {
                            int days = Integer.parseInt(text.replaceAll("[^0-9]", ""));
                            wizard.buildingContest.startDate = Instant.now().plusSeconds(days * 24L * 60 * 60);
                            wizard.stage = AdminStage.WAIT_CONTEST_END;
                            sendMessage(chatId, "✅ Boshlanish vaqti saqlandi!\n\nTugash vaqti (kun soni):", null);
                        } catch (Exception e) {
                            sendMessage(chatId, "❌ Noto'g'ri format!", null);
                        }
                    }
                    return true;

                case WAIT_CONTEST_END:
                    try {
                        int days = Integer.parseInt(text.replaceAll("[^0-9]", ""));
                        wizard.buildingContest.endDate = wizard.buildingContest.startDate.plusSeconds(days * 24L * 60 * 60);
                        wizard.buildingContest.active = true;
                        CONTESTS.put(wizard.buildingContest.id, wizard.buildingContest);

                        sendMessage(chatId, "✅ <b>KONKURS YARATILDI!</b>\n\n" +
                                "📌 Nomi: " + wizard.buildingContest.name + "\n" +
                                "🎁 Sovrin: " + wizard.buildingContest.prize + "\n" +
                                "👥 G'oliblar: " + wizard.buildingContest.winnerCount, null);

                        ADMIN_WIZ.remove(userId);
                        sendAdminMenu(chatId);
                    } catch (Exception e) {
                        sendMessage(chatId, "❌ Noto'g'ri format!", null);
                    }
                    return true;

                case WAIT_CHANNEL_ADD:
                    String[] parts = text.split("\\|");
                    if (parts.length < 3) {
                        sendMessage(chatId, "❌ Format: username | link | nom", null);
                        return true;
                    }
                    REQUIRED_CHANNELS.add(new RequiredChannel(
                            parts[0].trim(), parts[1].trim(), parts[2].trim(), true
                    ));
                    sendMessage(chatId, "✅ Kanal qo'shildi: " + parts[2].trim(), null);
                    ADMIN_WIZ.remove(userId);
                    sendAdminMenu(chatId);
                    return true;

                case WAIT_CHANNEL_REMOVE:
                    try {
                        int index = Integer.parseInt(text) - 1;
                        if (index >= 0 && index < REQUIRED_CHANNELS.size()) {
                            RequiredChannel removed = REQUIRED_CHANNELS.remove(index);
                            sendMessage(chatId, "✅ Kanal o'chirildi: " + removed.name, null);
                        } else {
                            sendMessage(chatId, "❌ Noto'g'ri indeks!", null);
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "❌ Noto'g'ri format!", null);
                        return true;
                    }
                    ADMIN_WIZ.remove(userId);
                    sendAdminMenu(chatId);
                    return true;

                default:
                    return false;
            }
        }

        // ============================================
        // ADMIN MENU METHODS
        // ============================================

        private void sendAdminMenu(long chatId) {
            if (!isAdmin(chatId)) {
                sendMessage(chatId, "❌ Siz admin emassiz!",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")))));
                return;
            }

            String text = "👑 <b>ADMIN PANELI</b>\n\n" +
                    "📊 Foydalanuvchilar: " + USERS.size() + "\n" +
                    "👑 Adminlar: " + ADMINS.size() + "\n" +
                    "📋 Kanallar: " + REQUIRED_CHANNELS.size() + "\n" +
                    "🎁 Droplar: " + DROPS.size() + "\n" +
                    "🏆 Konkurslar: " + CONTESTS.size() + "\n\n" +
                    "⚙️ Amallarni tanlang:";

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(Arrays.asList(
                    createButton("📋 Adminlar", "admin_list"),
                    createButton("➕ Qo'shish", "admin_add")
            ));
            rows.add(Arrays.asList(
                    createButton("➖ O'chirish", "admin_remove"),
                    createButton("📊 Statistika", "menu_stats")
            ));
            rows.add(Arrays.asList(
                    createButton("🎁 Drop boshqaruvi", "admin_drop_menu"),
                    createButton("🏆 Konkurslar", "admin_contest_menu")
            ));
            rows.add(Arrays.asList(
                    createButton("📢 Yangilik yuborish", "admin_broadcast"),
                    createButton("⚙️ Sozlamalar", "admin_settings")
            ));
            rows.add(Arrays.asList(
                    createButton("🆕 Yangi Drop", "admin_new_drop"),
                    createButton("🆕 Yangi Konkurs", "admin_new_contest")
            ));
            rows.add(Arrays.asList(
                    createButton("⬅️ Orqaga", "menu_main")
            ));

            sendMessage(chatId, text, createKeyboard(rows));
        }

        private void showAdminList(long chatId, int msgId, boolean hasPhoto) {
            StringBuilder text = new StringBuilder("👑 <b>ADMINLAR</b>\n\n");
            for (Map.Entry<Long, AdminInfo> entry : ADMINS.entrySet()) {
                AdminInfo a = entry.getValue();
                text.append("• ").append(a.id).append(" - ").append(a.name)
                        .append(a.isSuperAdmin ? " ⭐" : "").append("\n");
            }

            List<List<InlineKeyboardButton>> rows = Arrays.asList(
                    Arrays.asList(createButton("➕ Qo'shish", "admin_add")),
                    Arrays.asList(createButton("⬅️ Orqaga", "admin_menu"))
            );
            editMessage(chatId, msgId, text.toString(), createKeyboard(rows), hasPhoto);
        }

        private void startAddAdmin(long chatId, int msgId, long userId, boolean hasPhoto) {
            if (!isSuperAdmin(userId)) {
                editMessage(chatId, msgId, "❌ Faqat SUPER ADMIN admin qo'sha oladi!",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "admin_menu")))), hasPhoto);
                return;
            }

            AdminWizard wizard = new AdminWizard(chatId, msgId);
            wizard.stage = AdminStage.WAIT_ADD_ADMIN;
            ADMIN_WIZ.put(userId, wizard);

            editMessage(chatId, msgId, "➕ <b>ADMIN QO'SHISH</b>\n\nTelegram ID yuboring:", null, hasPhoto);
        }

        private void startRemoveAdmin(long chatId, int msgId, long userId, boolean hasPhoto) {
            if (!isSuperAdmin(userId)) {
                editMessage(chatId, msgId, "❌ Faqat SUPER ADMIN admin o'chira oladi!",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "admin_menu")))), hasPhoto);
                return;
            }

            AdminWizard wizard = new AdminWizard(chatId, msgId);
            wizard.stage = AdminStage.WAIT_REMOVE_ADMIN;
            ADMIN_WIZ.put(userId, wizard);

            editMessage(chatId, msgId, "➖ <b>ADMIN O'CHIRISH</b>\n\nAdmin ID yuboring:", null, hasPhoto);
        }

        private void showAdminDropMenu(long chatId, int msgId, long userId, boolean hasPhoto) {
            Drop active = getActiveDrop();
            String text = "🎁 <b>DROP BOSHQARUVI</b>\n\n" +
                    "Aktiv drop: " + (active != null ? active.name : "yo'q") + "\n" +
                    "Jami droplar: " + DROPS.size();

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(Arrays.asList(createButton("🆕 Yangi Drop", "admin_new_drop")));
            rows.add(Arrays.asList(createButton("📋 Droplar ro'yxati", "admin_list_drops")));
            rows.add(Arrays.asList(
                    createButton("✅ Ochish", "admin_open_drop"),
                    createButton("⛔ Yopish", "admin_close_drop")
            ));
            rows.add(Arrays.asList(createButton("⬅️ Orqaga", "admin_menu")));

            editMessage(chatId, msgId, text, createKeyboard(rows), hasPhoto);
        }

        private void adminListDrops(long chatId, int msgId, long userId, boolean hasPhoto) {
            if (!isAdmin(userId)) return;

            List<Drop> drops = new ArrayList<>(DROPS.values());
            drops.sort((a, b) -> Long.compare(b.id, a.id));

            if (drops.isEmpty()) {
                editMessage(chatId, msgId, "❌ Drop mavjud emas",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "admin_drop_menu")))), hasPhoto);
                return;
            }

            StringBuilder sb = new StringBuilder("📋 <b>DROPLAR</b>\n\n");
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (int i = 0; i < Math.min(5, drops.size()); i++) {
                Drop d = drops.get(i);
                sb.append(d.active ? "✅ " : "⭕ ").append("<b>").append(d.name).append("</b>\n");
                sb.append("  • Kodlar: ").append(d.claimedCount).append("/").append(d.totalCodes).append("\n\n");

                String action = d.active ? "⛔ Yopish" : "✅ Ochish";
                rows.add(Arrays.asList(
                        createButton(action + " " + (d.name.length() > 10 ? d.name.substring(0, 8) + "..." : d.name),
                                (d.active ? "close_" : "open_") + d.id)
                ));
            }
            rows.add(Arrays.asList(createButton("⬅️ Orqaga", "admin_drop_menu")));

            editMessage(chatId, msgId, sb.toString(), createKeyboard(rows), hasPhoto);
        }

        private void adminOpenDrop(long chatId, int msgId, long userId, boolean hasPhoto) {
            Drop last = DROPS.values().stream().max(Comparator.comparingLong(d -> d.id)).orElse(null);
            if (last != null) {
                DROPS.values().forEach(d -> d.active = false);
                last.active = true;
                editMessage(chatId, msgId, "✅ Drop ochildi: " + last.name,
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "admin_drop_menu")))), hasPhoto);
            } else {
                editMessage(chatId, msgId, "❌ Drop mavjud emas",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "admin_drop_menu")))), hasPhoto);
            }
        }

        private void adminCloseDrop(long chatId, int msgId, long userId, boolean hasPhoto) {
            Drop active = getActiveDrop();
            if (active != null) {
                active.active = false;
                editMessage(chatId, msgId, "⛔ Drop yopildi: " + active.name,
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "admin_drop_menu")))), hasPhoto);
            } else {
                editMessage(chatId, msgId, "❌ Aktiv drop mavjud emas",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "admin_drop_menu")))), hasPhoto);
            }
        }

        private void showContestsMenu(long chatId, int msgId, long userId, boolean hasPhoto) {
            List<Contest> contests = new ArrayList<>(CONTESTS.values());
            contests.sort((a, b) -> b.createdAt.compareTo(a.createdAt));

            StringBuilder text = new StringBuilder("🏆 <b>KONKURSLAR</b>\n\n");
            if (contests.isEmpty()) {
                text.append("Konkurs mavjud emas.");
            } else {
                for (int i = 0; i < Math.min(3, contests.size()); i++) {
                    Contest c = contests.get(i);
                    text.append((i+1) + ". <b>" + c.name + "</b>\n");
                    text.append("   • Holat: " + (c.active ? "🟢 Aktiv" : "⏸ To'xtatilgan") + "\n");
                    text.append("   • Qatnashchilar: " + c.participants.size() + "\n\n");
                }
            }

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(Arrays.asList(
                    createButton("🆕 Yangi", "admin_new_contest"),
                    createButton("📋 Barchasi", "admin_list_contests")
            ));
            rows.add(Arrays.asList(createButton("⬅️ Orqaga", "admin_menu")));

            editMessage(chatId, msgId, text.toString(), createKeyboard(rows), hasPhoto);
        }

        private void startBroadcast(long chatId, int msgId, long userId, boolean hasPhoto) {
            AdminWizard wizard = new AdminWizard(chatId, msgId);
            wizard.stage = AdminStage.WAIT_BROADCAST_MESSAGE;
            ADMIN_WIZ.put(userId, wizard);

            editMessage(chatId, msgId, "📢 <b>YANGILIK YUBORISH</b>\n\nXabar matnini yozing:", null, hasPhoto);
        }

        private void adminNewDrop(long chatId, int msgId, long userId, boolean hasPhoto) {
            AdminWizard wizard = new AdminWizard(chatId, msgId);
            wizard.stage = AdminStage.WAIT_DROP_NAME;
            wizard.building = new Drop(LAST_DROP_ID.incrementAndGet());
            wizard.building.createdBy = userId;
            ADMIN_WIZ.put(userId, wizard);

            editMessage(chatId, msgId, "🆕 <b>YANGI DROP</b>\n\n1/4. Drop nomini kiriting:", null, hasPhoto);
        }

        private void adminNewContest(long chatId, int msgId, long userId, boolean hasPhoto) {
            AdminWizard wizard = new AdminWizard(chatId, msgId);
            wizard.stage = AdminStage.WAIT_CONTEST_NAME;
            wizard.buildingContest = new Contest(LAST_CONTEST_ID.incrementAndGet(), userId);
            ADMIN_WIZ.put(userId, wizard);

            editMessage(chatId, msgId, "🆕 <b>YANGI KONKURS</b>\n\n1/6. Konkurs nomini kiriting:", null, hasPhoto);
        }

        private void showSettings(long chatId, int msgId, long userId, boolean hasPhoto) {
            String text = "⚙️ <b>SOZLAMALAR</b>\n\n" +
                    "📢 Kanallar: " + REQUIRED_CHANNELS.size() + "\n" +
                    "🌐 Til: " + botSettings.language + "\n" +
                    "✅ Kanal tekshirish: " + (botSettings.requireJoin ? "Yoqilgan" : "O'chirilgan") + "\n" +
                    "🛡 Anti-spam: " + (botSettings.antiSpam ? botSettings.spamTimeSeconds + " sek" : "O'chirilgan") + "\n" +
                    "📊 Kunlik limit: " + botSettings.maxDailyClaims;

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(Arrays.asList(
                    createButton("➕ Kanal qo'shish", "settings_add_channel"),
                    createButton("➖ Kanal o'chirish", "settings_remove_channel")
            ));
            rows.add(Arrays.asList(createButton("⬅️ Orqaga", "admin_menu")));

            editMessage(chatId, msgId, text, createKeyboard(rows), hasPhoto);
        }

        private void startAddChannel(long chatId, int msgId, long userId, boolean hasPhoto) {
            AdminWizard wizard = new AdminWizard(chatId, msgId);
            wizard.stage = AdminStage.WAIT_CHANNEL_ADD;
            ADMIN_WIZ.put(userId, wizard);

            editMessage(chatId, msgId, "➕ <b>KANAL QO'SHISH</b>\n\nFormat: username | link | nom\nMisol: @kanal | https://t.me/kanal | Kanal nomi", null, hasPhoto);
        }

        private void startRemoveChannel(long chatId, int msgId, long userId, boolean hasPhoto) {
            StringBuilder text = new StringBuilder("➖ <b>KANAL O'CHIRISH</b>\n\n");
            for (int i = 0; i < REQUIRED_CHANNELS.size(); i++) {
                text.append((i+1) + ". " + REQUIRED_CHANNELS.get(i).name + "\n");
            }
            text.append("\nRaqam kiriting:");

            AdminWizard wizard = new AdminWizard(chatId, msgId);
            wizard.stage = AdminStage.WAIT_CHANNEL_REMOVE;
            ADMIN_WIZ.put(userId, wizard);

            editMessage(chatId, msgId, text.toString(), null, hasPhoto);
        }

        private void handleDropAction(long chatId, int msgId, long userId, String data, boolean hasPhoto) {
            try {
                if (data.startsWith("open_")) {
                    long id = Long.parseLong(data.substring(5));
                    Drop drop = DROPS.get(id);
                    if (drop != null) {
                        DROPS.values().forEach(d -> d.active = false);
                        drop.active = true;
                    }
                } else if (data.startsWith("close_")) {
                    long id = Long.parseLong(data.substring(6));
                    Drop drop = DROPS.get(id);
                    if (drop != null) drop.active = false;
                }
            } catch (Exception ignored) {}
            adminListDrops(chatId, msgId, userId, hasPhoto);
        }

        // ============================================
        // USER MENU METHODS
        // ============================================

        private void showProfile(long chatId, int msgId, long userId, boolean hasPhoto) {
            User user = USERS.computeIfAbsent(userId, User::new);
            Drop drop = getActiveDrop();

            String text = "👤 <b>PROFIL</b>\n\n" +
                    "🆔 ID: <code>" + userId + "</code>\n" +
                    "👤 Ism: " + (user.firstName != null ? user.firstName : "Noma'lum") + "\n" +
                    "📛 Username: " + (user.username != null ? "@" + user.username : "Yo'q") + "\n" +
                    "👥 Referallar: " + user.refCount + "\n" +
                    "🏆 Kodlar: " + user.totalCodesClaimed + "\n" +
                    "🏅 Yutuqlar: " + user.totalWins + "\n\n" +
                    "🎁 Aktiv drop: " + (drop != null ? drop.name : "Yo'q");

            List<List<InlineKeyboardButton>> rows = Arrays.asList(
                    Arrays.asList(createButton("🔗 Referral Link", "menu_ref")),
                    Arrays.asList(createButton("⬅️ Orqaga", "menu_main"))
            );

            editMessage(chatId, msgId, text, createKeyboard(rows), hasPhoto);
        }

        private void showRefLink(long chatId, int msgId, long userId, boolean hasPhoto) {
            String link = "https://t.me/" + BOT_USERNAME + "?start=" + userId;
            User user = USERS.get(userId);

            String text = "🔗 <b>REFERRAL LINK</b>\n\n" +
                    "<code>" + link + "</code>\n\n" +
                    "👥 Referallar: " + (user != null ? user.refCount : 0);

            List<List<InlineKeyboardButton>> rows = Arrays.asList(
                    Arrays.asList(createButton("📋 Nusxalash", "copy_link")),
                    Arrays.asList(createButton("⬅️ Orqaga", "menu_main"))
            );

            editMessage(chatId, msgId, text, createKeyboard(rows), hasPhoto);
        }

        private void showContestsList(long chatId, int msgId, long userId, boolean hasPhoto) {
            List<Contest> activeContests = getActiveContests();
            if (activeContests.isEmpty()) {
                editMessage(chatId, msgId, "🏆 <b>KONKURSLAR</b>\n\nHozircha aktiv konkurs mavjud emas.",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")))), hasPhoto);
                return;
            }

            StringBuilder text = new StringBuilder("🏆 <b>AKTIV KONKURSLAR</b>\n\n");
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            User user = USERS.get(userId);

            for (Contest c : activeContests) {
                boolean joined = c.participants.contains(userId);
                text.append(joined ? "✅ " : "⭕ ").append("<b>").append(c.name).append("</b>\n");
                text.append("  🎁 Sovrin: ").append(c.prize).append("\n");
                text.append("  👥 Qatnashchilar: ").append(c.participants.size()).append("\n\n");

                if (!joined && user.refCount >= c.requiredRefs) {
                    rows.add(Arrays.asList(
                            createButton("✅ Qatnashish", "join_contest_" + c.id)
                    ));
                }
            }
            rows.add(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")));

            editMessage(chatId, msgId, text.toString(), createKeyboard(rows), hasPhoto);
        }

        private void joinContest(long chatId, int msgId, long userId, long contestId, boolean hasPhoto) {
            Contest contest = CONTESTS.get(contestId);
            if (contest == null || contest.finished || !contest.active) {
                answerCallback(String.valueOf(chatId), "❌ Konkurs topilmadi!", false);
                showContestsList(chatId, msgId, userId, hasPhoto);
                return;
            }

            User user = USERS.get(userId);
            if (user == null) return;

            if (user.refCount < contest.requiredRefs) {
                answerCallback(String.valueOf(chatId), "❌ Yetarli referral yo'q!", true);
                return;
            }

            if (contest.participants.add(userId)) {
                user.totalContestsParticipated++;
                answerCallback(String.valueOf(chatId), "✅ Qatnashdingiz! Omad!", false);
                showContestsList(chatId, msgId, userId, hasPhoto);
            } else {
                answerCallback(String.valueOf(chatId), "❌ Allaqachon qatnashgansiz!", false);
            }
        }

        private void showDropMenu(long chatId, int msgId, long userId, boolean hasPhoto) {
            Drop drop = getActiveDrop();
            if (drop == null) {
                editMessage(chatId, msgId, "❌ Aktiv drop mavjud emas!",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")))), hasPhoto);
                return;
            }

            User user = USERS.get(userId);
            int refs = user != null ? user.refCount : 0;
            boolean claimed = drop.claimedUsers.contains(userId);
            boolean canClaim = !claimed && refs >= drop.requiredRefs && !drop.codes.isEmpty();

            String text = "🎁 <b>" + drop.name + "</b>\n\n" +
                    "📝 " + (drop.description.isEmpty() ? "Izoh yo'q" : drop.description) + "\n\n" +
                    "🎯 Talab: " + drop.requiredRefs + " referral\n" +
                    "👥 Sizda: " + refs + " referral\n" +
                    "🔑 Qolgan kodlar: " + drop.codes.size() + "/" + drop.totalCodes + "\n\n" +
                    (claimed ? "✅ Siz allaqachon oldingiz" :
                            drop.codes.isEmpty() ? "❌ Kodlar tugagan" :
                                    canClaim ? "🎯 Kod olishingiz mumkin!" :
                                            "⏳ Yana " + (drop.requiredRefs - refs) + " referral kerak");

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            if (canClaim) {
                rows.add(Arrays.asList(createButton("🚀 Promo Kod Olish", "claim_code")));
            }
            rows.add(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")));

            editMessage(chatId, msgId, text, createKeyboard(rows), hasPhoto);
        }

        private void claimCode(long chatId, int msgId, long userId, boolean hasPhoto) {
            Drop drop = getActiveDrop();
            if (drop == null) {
                editMessage(chatId, msgId, "❌ Aktiv drop yo'q!",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")))), hasPhoto);
                return;
            }

            User user = USERS.computeIfAbsent(userId, User::new);

            if (botSettings.maxDailyClaims > 0 && user.dailyClaims >= botSettings.maxDailyClaims) {
                editMessage(chatId, msgId, "❌ Kunlik limit tugadi!",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")))), hasPhoto);
                return;
            }

            dropLock.lock();
            try {
                if (drop.claimedUsers.contains(userId)) {
                    editMessage(chatId, msgId, "❌ Siz allaqachon kod oldingiz!",
                            createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")))), hasPhoto);
                    return;
                }

                if (user.refCount < drop.requiredRefs) {
                    editMessage(chatId, msgId, "⏳ Yana " + (drop.requiredRefs - user.refCount) + " referral kerak!",
                            createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")))), hasPhoto);
                    return;
                }

                if (drop.codes.isEmpty()) {
                    editMessage(chatId, msgId, "❌ Kodlar tugagan!",
                            createKeyboard(Arrays.asList(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")))), hasPhoto);
                    return;
                }

                String code = drop.codes.removeFirst();
                drop.claimedUsers.add(userId);
                drop.claimedCount++;
                user.totalCodesClaimed++;
                user.dailyClaims++;

                editMessage(chatId, msgId, "🎉 <b>TABRIKLAYMIZ!</b>\n\n🔑 <code>" + code + "</code>\n\n✅ Kod nusxalandi!",
                        createKeyboard(Arrays.asList(Arrays.asList(createButton("🏠 Bosh menu", "menu_main")))), hasPhoto);

            } finally {
                dropLock.unlock();
            }
        }

        private void showStats(long chatId, int msgId, boolean hasPhoto) {
            long uptimeMs = System.currentTimeMillis() - BOT_START_TIME.toEpochMilli();
            long hours = uptimeMs / 3600000;
            long minutes = (uptimeMs / 60000) % 60;

            String text = "📊 <b>STATISTIKA</b>\n\n" +
                    "👥 Foydalanuvchilar: " + USERS.size() + "\n" +
                    "👑 Adminlar: " + ADMINS.size() + "\n" +
                    "🎁 Droplar: " + DROPS.size() + "\n" +
                    "🏆 Konkurslar: " + CONTESTS.size() + "\n" +
                    "🔑 Berilgan kodlar: " + DROPS.values().stream().mapToInt(d -> d.claimedCount).sum() + "\n" +
                    "⏱ Ish vaqti: " + hours + " soat " + minutes + " daqiqa";

            List<List<InlineKeyboardButton>> rows = Arrays.asList(
                    Arrays.asList(createButton("🔄 Yangilash", "menu_stats")),
                    Arrays.asList(createButton("⬅️ Orqaga", "menu_main"))
            );

            editMessage(chatId, msgId, text, createKeyboard(rows), hasPhoto);
        }

        private void showHelp(long chatId, int msgId, boolean hasPhoto) {
            String text = "🆘 <b>YORDAM</b>\n\n" +
                    "Komandalar:\n" +
                    "• /start - boshlash\n" +
                    "• /help - yordam\n" +
                    "• /stats - statistika\n" +
                    "• /admin - admin panel\n" +
                    "• /cancel - bekor qilish\n\n" +
                    "Qanday ishlaydi:\n" +
                    "1. Kanallarga azo bo'ling\n" +
                    "2. Do'stlarni taklif qiling\n" +
                    "3. Referral to'plang\n" +
                    "4. Kod oling yoki konkursda qatnashing";

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (RequiredChannel ch : REQUIRED_CHANNELS) {
                if (ch.active) {
                    rows.add(Arrays.asList(
                            InlineKeyboardButton.builder().text("📢 " + ch.name).url(ch.link).build()
                    ));
                }
            }
            rows.add(Arrays.asList(createButton("⬅️ Orqaga", "menu_main")));

            editMessage(chatId, msgId, text, createKeyboard(rows), hasPhoto);
        }

        // ============================================
        // CALLBACK HANDLER
        // ============================================

        private void handleCallback(CallbackQuery callback) {
            Message message = (Message) callback.getMessage();
            long chatId = message.getChatId();
            long userId = callback.getFrom().getId();
            int msgId = message.getMessageId();
            String data = callback.getData();
            boolean hasPhoto = message.hasPhoto();

            if (!"check_join".equals(data) && !isJoinedAllChannels(userId)) {
                sendJoinChannelsMessage(chatId);
                answerCallback(callback.getId(), null, false);
                return;
            }

            switch (data) {
                case "check_join":
                    if (isJoinedAllChannels(userId)) {
                        sendMainMenu(chatId);
                    } else {
                        sendJoinChannelsMessage(chatId);
                    }
                    break;

                case "menu_main":
                    sendMainMenu(chatId);
                    break;

                case "menu_drop":
                    showDropMenu(chatId, msgId, userId, hasPhoto);
                    break;

                case "menu_profile":
                    showProfile(chatId, msgId, userId, hasPhoto);
                    break;

                case "menu_ref":
                    showRefLink(chatId, msgId, userId, hasPhoto);
                    break;

                case "menu_stats":
                    showStats(chatId, msgId, hasPhoto);
                    break;

                case "menu_help":
                    showHelp(chatId, msgId, hasPhoto);
                    break;

                case "menu_contests":
                    showContestsList(chatId, msgId, userId, hasPhoto);
                    break;

                case "menu_admin":
                case "admin_menu":
                    sendAdminMenu(chatId);
                    break;

                case "admin_list":
                    showAdminList(chatId, msgId, hasPhoto);
                    break;

                case "admin_add":
                    startAddAdmin(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_remove":
                    startRemoveAdmin(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_drop_menu":
                    showAdminDropMenu(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_contest_menu":
                    showContestsMenu(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_settings":
                    showSettings(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_broadcast":
                    startBroadcast(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_new_drop":
                    adminNewDrop(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_new_contest":
                    adminNewContest(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_list_drops":
                    adminListDrops(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_open_drop":
                    adminOpenDrop(chatId, msgId, userId, hasPhoto);
                    break;

                case "admin_close_drop":
                    adminCloseDrop(chatId, msgId, userId, hasPhoto);
                    break;

                case "settings_add_channel":
                    startAddChannel(chatId, msgId, userId, hasPhoto);
                    break;

                case "settings_remove_channel":
                    startRemoveChannel(chatId, msgId, userId, hasPhoto);
                    break;

                case "copy_link":
                    answerCallback(callback.getId(), "✅ Link nusxalandi!", false);
                    break;

                case "claim_code":
                    claimCode(chatId, msgId, userId, hasPhoto);
                    break;

                default:
                    if (data.startsWith("open_") || data.startsWith("close_")) {
                        handleDropAction(chatId, msgId, userId, data, hasPhoto);
                    } else if (data.startsWith("join_contest_")) {
                        long contestId = Long.parseLong(data.substring(13));
                        joinContest(chatId, msgId, userId, contestId, hasPhoto);
                    }
                    break;
            }

            answerCallback(callback.getId(), null, false);
        }
    }
}