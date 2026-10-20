package freshlink;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Main FreshLink inventory and food rescue workspace. */
public final class FreshLinkApp {
    private static final Color INK = new Color(31, 48, 42);
    private static final Color MUTED = new Color(119, 132, 124);
    private static Color GREEN = new Color(22, 163, 74);
    private static float fontScale=1f;
    private static final Color PALE = new Color(246, 248, 245);
    private static final Color LINE = new Color(231, 236, 231);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final FoodStore store = new FoodStore();
    private final NotificationStore notificationStore = new NotificationStore();
    private final SettingsStore settings = new SettingsStore();
    private UserStore userStore;
    private User currentUser;
    private final java.util.List<User> users = new ArrayList<>();
    private final List<AppNotification> notifications = new ArrayList<>();
    private final List<JLabel> reportValueLabels = new ArrayList<>();
    private final List<java.util.function.LongSupplier> reportSuppliers = new ArrayList<>();
    private final List<FoodItem> items = new ArrayList<>();
    private final JFrame frame = new JFrame("FreshLink | Food rescue, made simple");
    private final ListingModel model = new ListingModel();
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField();
    private final JLabel availableValue = new JLabel("0");
    private final JLabel urgentValue = new JLabel("0");
    private final JLabel rescuedValue = new JLabel("0");
    private final JLabel activeValue = new JLabel("0");
    private final JLabel reservedValue = new JLabel("0");
    private final JLabel tableCount = new JLabel("0 listings");
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageHost = new JPanel(pageLayout);
    private final JPanel listingCards = new JPanel(new GridLayout(0, 3, 14, 14));
    private final CardLayout listingModeLayout = new CardLayout();
    private final JPanel listingModes = new JPanel(listingModeLayout);
    private JLabel pageTitle;
    private JButton notificationButton;
    private JPanel notificationPanel;
    private JPanel notificationBody;
    private JLabel notificationSummary;
    private String activeFilter = "All listings";
    private String activePage = "Dashboard";
    private String viewMode = "Cards";
    private String selectedCategory = "All categories";

    public FreshLinkApp() {
        GREEN=accentColor(settings.get("accent","Green"));fontScale=switch(settings.get("fontSize","Medium")){case "Small"->0.92f;case "Large"->1.12f;default->1f;};
        try {
            userStore = new UserStore(); users.addAll(userStore.load());
            notifications.addAll(notificationStore.load());
            if (store.hasSavedInventory()) items.addAll(store.load());
            else {
                items.addAll(DemoData.create());
                store.save(items);
            }
            if(isLegacyDemoInventory()){
                for(FoodItem demo:DemoData.create())if(items.stream().noneMatch(i->i.getName().equalsIgnoreCase(demo.getName())&&i.getDonor().equalsIgnoreCase(demo.getDonor())))items.add(demo);
                store.save(items);
            }
        }
        catch (Exception ex) { SwingUtilities.invokeLater(() -> toast("Saved listings could not be loaded.")); }
        for(FoodItem i:items)if(settings.getBoolean("autoExpiry",true)&&!i.isClaimed()&&!i.isReserved()&&(i.isExpired()||i.expiresSoon()))for(User u:users)if(u.role().equals("Admin")){
            String message=i.isExpired()?i.getName()+" has passed its best-before date and needs review.":i.getName()+" is approaching its best-before date.";
            if(notifications.stream().noneMatch(n->n.recipient().equals(u.name())&&n.message().equals(message)))notifyPerson(u.name(),message);
        }
    }

    public void show() {
        if (!login()) return;
        buildWindow(); refresh(); frame.setVisible(true);
    }

    private boolean login() {
        JDialog dialog=new JDialog((Frame)null,"FreshLink",true);dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout());root.setBackground(PALE);
        JPanel hero=new JPanel(new BorderLayout());hero.setBackground(new Color(15,72,54));hero.setBorder(new EmptyBorder(25,30,24,30));
        JPanel heroWords=new JPanel();heroWords.setOpaque(false);heroWords.setLayout(new BoxLayout(heroWords,BoxLayout.Y_AXIS));
        heroWords.add(label("FreshLink",25,Color.WHITE,Font.BOLD));heroWords.add(Box.createVerticalStrut(6));heroWords.add(label("Rescue food. Feed community.",13,new Color(213,236,221),Font.PLAIN));hero.add(heroWords,BorderLayout.WEST);
        JLabel mark=label("✳",34,new Color(213,236,221),Font.BOLD);hero.add(mark,BorderLayout.EAST);root.add(hero,BorderLayout.NORTH);
        JPanel form=new JPanel();form.setBackground(PALE);form.setBorder(new EmptyBorder(28,34,26,34));form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        form.add(label("Welcome back",21,INK,Font.BOLD));form.add(Box.createVerticalStrut(5));form.add(label("Sign in to your local rescue workspace.",12,MUTED,Font.PLAIN));form.add(Box.createVerticalStrut(22));
        form.add(label("EMAIL ADDRESS",10,INK,Font.BOLD));form.add(Box.createVerticalStrut(6));JTextField email=inputField("you@organization.org");email.setText("admin@freshlink.app");email.setMaximumSize(new Dimension(420,40));form.add(email);form.add(Box.createVerticalStrut(15));
        form.add(label("PASSWORD",10,INK,Font.BOLD));form.add(Box.createVerticalStrut(6));JPasswordField password=new JPasswordField();password.setFont(new Font("SansSerif",Font.PLAIN,13));password.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),new EmptyBorder(9,10,9,10)));password.setMaximumSize(new Dimension(420,40));form.add(password);
        JLabel error=label("",11,new Color(190,49,49),Font.PLAIN);error.setBorder(new EmptyBorder(8,0,0,0));form.add(error);form.add(Box.createVerticalStrut(12));
        JButton signIn=primaryButton("Sign in to FreshLink");signIn.setAlignmentX(Component.LEFT_ALIGNMENT);signIn.setMaximumSize(new Dimension(420,45));form.add(signIn);form.add(Box.createVerticalStrut(10));
        JButton register=secondaryButton("Create a Donor or Collector account");register.setAlignmentX(Component.LEFT_ALIGNMENT);form.add(register);form.add(Box.createVerticalStrut(18));
        JPanel demo=new JPanel(new BorderLayout());demo.setBackground(new Color(237,245,239));demo.setBorder(new EmptyBorder(12,14,12,14));
        demo.add(label("DEMO ADMIN ACCESS",9,GREEN,Font.BOLD),BorderLayout.NORTH);demo.add(label("admin@freshlink.app  ·  admin123",11,INK,Font.PLAIN),BorderLayout.CENTER);form.add(demo);
        Runnable attempt=()->{String address=email.getText().trim().toLowerCase(Locale.ROOT),pass=new String(password.getPassword());
            for(User u:users)if(u.email().equalsIgnoreCase(address)&&UserStore.hash(pass,u.salt()).equals(u.passwordHash())){currentUser=u;dialog.dispose();return;}
            error.setText("We couldn't match that email and password. Please try again.");};
        signIn.addActionListener(e->attempt.run());password.addActionListener(e->attempt.run());register.addActionListener(e->{if(register()) {dialog.dispose();}});
        root.add(form,BorderLayout.CENTER);dialog.setContentPane(root);dialog.setSize(460,610);dialog.setResizable(false);dialog.setLocationRelativeTo(null);dialog.setVisible(true);
        return currentUser!=null;
    }

    private boolean isLegacyDemoInventory(){
        java.util.Set<String> old=java.util.Set.of("Rustic sourdough loaves","Seasonal vegetable boxes","Garden tomato soup","Plain yoghurt tubs","Ripe bananas","Brown lentils","Fresh herb focaccia","Apple and oat muffins","Harvest vegetable stew");
        return items.size()==9&&items.stream().allMatch(i->old.contains(i.getName()));
    }

    private boolean register() {
        JTextField name = new JTextField(22), email = new JTextField(22);
        JPasswordField password = new JPasswordField(22);
        JComboBox<String> role = new JComboBox<>(new String[]{"Donor", "Collector"});
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("Name or organization")); p.add(name); p.add(new JLabel("Email")); p.add(email);
        p.add(new JLabel("Password (at least 8 characters)")); p.add(password); p.add(new JLabel("Role")); p.add(role);
        if (JOptionPane.showConfirmDialog(null, p, "Create FreshLink account", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return false;
        String e = email.getText().trim().toLowerCase(Locale.ROOT), pw = new String(password.getPassword());
        if (name.getText().isBlank() || !e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || pw.length() < 8 || users.stream().anyMatch(u -> u.email().equalsIgnoreCase(e))) {
            JOptionPane.showMessageDialog(null, "Enter a name, a valid unused email, and a password of at least 8 characters."); return false;
        }
        String salt = UserStore.newSalt(); User u = new User(name.getText().trim(), e, role.getSelectedItem().toString(), salt, UserStore.hash(pw, salt));
        try { users.add(u); userStore.save(users); currentUser = u; return true; }
        catch (Exception ex) { JOptionPane.showMessageDialog(null, "Could not save account: " + ex.getMessage()); return false; }
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1120, 700));
        frame.setSize(1360, 800);
        frame.setLocationRelativeTo(null);
        frame.setBackground(PALE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PALE);
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(PALE);
        content.setBorder(new EmptyBorder(22, 28, 24, 28));
        content.add(buildHeader(), BorderLayout.NORTH);
        pageHost.setOpaque(false);
        pageHost.add(buildDashboardPage(), "Dashboard");
        pageHost.add(buildFoodBoardPage(), "Food board");
        pageHost.add(buildReportsPage(), "Reports");
        pageHost.add(buildNotificationsPage(), "Notifications");
        pageHost.add(buildSettingsPage(), "Settings");
        if (currentUser.role().equals("Admin")) pageHost.add(buildUsersPage(), "Users");
        content.add(pageHost, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        frame.setContentPane(root);
        installKeys();
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Color.WHITE);
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, LINE));
        side.setPreferredSize(new Dimension(238, 0));
        side.add(brand());

        JLabel section = label("WORKSPACE", 10, new Color(153, 164, 157), Font.BOLD);
        section.setBorder(new EmptyBorder(32, 24, 11, 0));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(section);
        side.add(navItem("⌂", "Dashboard", "Dashboard"));
        side.add(navItem("▦", "Food board", "Food board"));
        side.add(navItem("◷", "Urgent rescue", "Urgent rescue"));
        side.add(navItem("✓", "My reservations", "My reservations"));
        if (currentUser.role().equals("Donor") || currentUser.role().equals("Admin")) side.add(navItem("＋", "Add listing", "Add listing"));
        side.add(navItem("▤", "Reports", "Reports"));
        long unread=notifications.stream().filter(this::visibleToCurrent).filter(n->!n.read()).count();
        side.add(navItem("♧", "Notifications"+(unread>0?"  ·  "+unread:""), "Notifications"));
        if (currentUser.role().equals("Admin")) side.add(navItem("◉", "Users", "Users"));
        if (currentUser.role().equals("Admin")) side.add(navItem("⚙", "Settings", "Settings"));
        side.add(Box.createVerticalGlue());

        JPanel mission = new JPanel();
        mission.setLayout(new BoxLayout(mission, BoxLayout.Y_AXIS));
        mission.setBackground(new Color(241, 247, 242));
        mission.setBorder(new EmptyBorder(17, 17, 17, 17));
        mission.setMaximumSize(new Dimension(190, 138));
        mission.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sprout = label("✳  SMALL ACTION,", 10, GREEN, Font.BOLD);
        JLabel better = label("big difference.", 10, GREEN, Font.BOLD);
        JLabel msg = label("Every rescued meal", 11, MUTED, Font.PLAIN);
        JLabel msg2 = label("counts toward a better", 11, MUTED, Font.PLAIN);
        JLabel msg3 = label("food system.", 11, MUTED, Font.PLAIN);
        mission.add(sprout); mission.add(Box.createVerticalStrut(3)); mission.add(better);
        mission.add(Box.createVerticalStrut(11)); mission.add(msg); mission.add(msg2); mission.add(msg3);
        side.add(mission);
        side.add(Box.createVerticalStrut(20));
        JLabel version = label("FRESHLINK  ·  COMMUNITY", 9, new Color(162, 171, 164), Font.BOLD);
        version.setBorder(new EmptyBorder(0, 22, 22, 0));
        side.add(version);
        return side;
    }

    private JPanel brand() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 11, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(24, 21, 14, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel icon = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GREEN); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 13, 13); g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setOpaque(false); icon.setPreferredSize(new Dimension(37, 37));
        JLabel glyph = label("✳", 21, Color.WHITE, Font.BOLD); icon.add(glyph);
        JPanel words = new JPanel(); words.setOpaque(false); words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.add(label("freshlink", 20, INK, Font.BOLD));
        words.add(label("FOOD RESCUE NETWORK", 8, MUTED, Font.BOLD));
        p.add(icon); p.add(words);
        return p;
    }

    private JButton navItem(String glyph, String title, String filter) {
        JButton b = new JButton("  " + glyph + "     " + title);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setForeground(activePage.equals(filter) ? GREEN : new Color(84, 99, 90));
        b.setBackground(activePage.equals(filter) ? new Color(237, 245, 239) : Color.WHITE);
        b.setBorder(new EmptyBorder(12, 17, 12, 12));
        b.setFocusPainted(false); b.setContentAreaFilled(true);
        b.setMaximumSize(new Dimension(238, 44)); b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> {
            if (filter.equals("Add listing")) { showAddDialog(); return; }
            activePage = filter;
            String pageLabel=filter;
            if (filter.equals("Urgent rescue")) { activePage = "Food board"; activeFilter = "Urgent rescue"; }
            else if (filter.equals("My reservations")) { activePage="Food board";activeFilter="My reservations"; }
            else if (filter.equals("Food board")) activeFilter = "All listings";
            if (pageTitle != null) pageTitle.setText(pageLabel);
            pageLayout.show(pageHost, activePage); refresh();
        });
        return b;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JPanel titles = new JPanel(); titles.setOpaque(false); titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        pageTitle = label("Dashboard", 24, INK, Font.BOLD);
        titles.add(pageTitle);
        titles.add(Box.createVerticalStrut(6));
        titles.add(label("Community food rescue, coordinated in one place.", 12, MUTED, Font.PLAIN));
        header.add(titles, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        long unread=notifications.stream().filter(this::visibleToCurrent).filter(n->!n.read()).count();
        JButton bell=secondaryButton("♧  "+unread);notificationButton=bell;bell.setToolTipText("Open notifications");bell.addActionListener(e->{activePage="Notifications";pageTitle.setText("Notifications");pageLayout.show(pageHost,"Notifications");showNotifications();updateNav();});actions.add(bell);
        JLabel identity = label("●  "+currentUser.name() + " · " + currentUser.role(), 11, MUTED, Font.BOLD);
        actions.add(identity);
        JButton add = primaryButton("＋  Add a listing");
        add.setEnabled(currentUser.role().equals("Donor") || currentUser.role().equals("Admin"));
        add.addActionListener(e -> showAddDialog()); actions.add(add);
        JButton logout = textButton("Log out"); logout.addActionListener(e -> { frame.dispose(); new FreshLinkApp().show(); }); actions.add(logout);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildDashboardPage() {
        JPanel page = new JPanel(new BorderLayout(0, 18)); page.setOpaque(false);
        JPanel top = buildOverview();
        JPanel lower = new JPanel(new GridLayout(1, 2, 16, 0)); lower.setOpaque(false);
        lower.add(chartCard("Listings by category", new CategoryChart()));
        lower.add(chartCard("Rescue pipeline", new StatusChart()));
        JPanel bottom = new JPanel(new GridLayout(1, 2, 16, 0)); bottom.setOpaque(false);
        bottom.add(buildUrgentPanel()); bottom.add(buildActivityPanel());
        JPanel all = new JPanel(new GridBagLayout()); all.setOpaque(false);
        GridBagConstraints row=new GridBagConstraints();row.gridx=0;row.weightx=1;row.fill=GridBagConstraints.HORIZONTAL;row.anchor=GridBagConstraints.NORTHWEST;row.insets=new Insets(0,0,16,0);
        row.gridy=0;all.add(top,row);row.gridy=1;all.add(lower,row);row.gridy=2;row.insets=new Insets(0,0,0,0);all.add(bottom,row);
        JScrollPane scroll=new JScrollPane(all);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getViewport().setOpaque(false);scroll.setOpaque(false);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(18);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildFoodBoardPage() { JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.add(buildListings()); return p; }

    private JPanel buildReservationsPage() {
        JPanel p = new JPanel(new BorderLayout(0, 16)); p.setOpaque(false);
        p.add(sectionHeading("Your collection queue", "Reservations assigned to " + currentUser.name()), BorderLayout.NORTH);
        JPanel panel = new Surface(new BorderLayout(0, 8)); panel.setBorder(new EmptyBorder(20, 22, 20, 22));
        JLabel copy = label(currentUser.role().equals("Collector") ? "Reserved food is held for your organization until you collect it or cancel the reservation." : "Use the Food Board to review reserved listings and manage pickup handoffs.", 13, MUTED, Font.PLAIN);
        panel.add(copy, BorderLayout.NORTH);
        JButton open = secondaryButton("Open reservation list"); open.addActionListener(e -> { activeFilter="My reservations"; activePage="Food board"; pageTitle.setText("My reservations"); pageLayout.show(pageHost,"Food board"); refreshTable(); });
        panel.add(open, BorderLayout.CENTER); p.add(panel, BorderLayout.CENTER); return p;
    }

    private JPanel buildReportsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16)); page.setOpaque(false);
        page.add(sectionHeading("Rescue impact", "A live summary of food shared through this workspace"), BorderLayout.NORTH);
        JPanel body = new JPanel(new GridLayout(2, 2, 14, 14)); body.setOpaque(false);
        body.add(reportMetric("LISTINGS CREATED", () -> items.size(), new Color(37,111,82)));
        body.add(reportMetric("SUCCESSFUL COLLECTIONS", () -> items.stream().filter(FoodItem::isClaimed).count(), new Color(37,99,190)));
        body.add(reportMetric("OPEN RESERVATIONS", () -> items.stream().filter(FoodItem::isReserved).count(), new Color(124,58,237)));
        body.add(reportMetric("ESTIMATED MEALS", () -> estimatedMeals(), new Color(234,116,36)));
        JPanel stack = new JPanel(new BorderLayout(0, 12)); stack.setOpaque(false); stack.add(body, BorderLayout.NORTH);
        JPanel activity = buildImpactByCategory(); stack.add(activity, BorderLayout.CENTER);
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false); head.add(sectionHeading("Reporting period", "All-time totals based on local inventory records"), BorderLayout.WEST);
        JButton export = secondaryButton("Export report CSV"); export.addActionListener(e -> exportReport()); head.add(export, BorderLayout.EAST);
        page.add(head, BorderLayout.NORTH); page.add(stack, BorderLayout.CENTER); return page;
    }

    private JPanel buildNotificationsPage() {
        JPanel page=new JPanel(new BorderLayout(0,16));page.setOpaque(false);page.add(sectionHeading("Notifications","Reservation, collection, and expiry updates for your organization"),BorderLayout.NORTH);
        JPanel panel=new Surface(new BorderLayout(0,12));panel.setBorder(new EmptyBorder(18,21,18,21));notificationPanel=panel;
        JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);long unread=notifications.stream().filter(n->visibleToCurrent(n)&&!n.read()).count();notificationSummary=label(unread+" unread · "+notifications.stream().filter(this::visibleToCurrent).count()+" total",11,MUTED,Font.PLAIN);head.add(notificationSummary,BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,5,0));actions.setOpaque(false);JButton read=secondaryButton("Mark all read");read.addActionListener(e->{for(int i=0;i<notifications.size();i++)if(visibleToCurrent(notifications.get(i)))notifications.set(i,notifications.get(i).markRead());saveNotifications();showNotifications();refresh();});
        JButton clear=textButton("Clear all");clear.addActionListener(e->{notifications.removeIf(this::visibleToCurrent);saveNotifications();showNotifications();refresh();});actions.add(read);actions.add(clear);head.add(actions,BorderLayout.EAST);panel.add(head,BorderLayout.NORTH);
        notificationBody=new JPanel(new BorderLayout());notificationBody.setOpaque(false);panel.add(notificationBody,BorderLayout.CENTER);
        page.add(panel,BorderLayout.CENTER);refreshNotifications();return page;
    }

    private void refreshNotifications(){
        if(notificationBody==null)return;notificationBody.removeAll();
        JPanel rows=new JPanel();rows.setOpaque(false);rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));List<AppNotification> visible=notifications.stream().filter(this::visibleToCurrent).sorted(Comparator.comparing(AppNotification::createdAt).reversed()).toList();
        if(visible.isEmpty())rows.add(label("You're all caught up. Listing updates will appear here.",13,MUTED,Font.PLAIN));
        long unread=visible.stream().filter(n->!n.read()).count();if(notificationSummary!=null)notificationSummary.setText(unread+" unread · "+visible.size()+" total");
        for(AppNotification n:visible){JPanel row=new JPanel(new BorderLayout(12,0));row.setOpaque(false);row.setBorder(new EmptyBorder(12,4,12,4));row.add(label(n.read()?"○":"●",12,n.read()?MUTED:GREEN,Font.BOLD),BorderLayout.WEST);JPanel copy=new JPanel();copy.setOpaque(false);copy.setLayout(new BoxLayout(copy,BoxLayout.Y_AXIS));copy.add(label(n.message(),12,INK,n.read()?Font.PLAIN:Font.BOLD));copy.add(Box.createVerticalStrut(4));copy.add(label(n.createdAt().format(DateTimeFormatter.ofPattern("MMM d · h:mm a")),10,MUTED,Font.PLAIN));row.add(copy,BorderLayout.CENTER);JButton mark=textButton(n.read()?"Read":"Mark read");mark.addActionListener(e->{int idx=notifications.indexOf(n);if(idx>=0)notifications.set(idx,n.markRead());saveNotifications();refreshNotifications();refresh();});row.add(mark,BorderLayout.EAST);rows.add(row);rows.add(new JSeparator());}
        JScrollPane scroll=new JScrollPane(rows);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.setOpaque(false);scroll.getViewport().setOpaque(false);notificationBody.add(scroll,BorderLayout.CENTER);notificationBody.revalidate();notificationBody.repaint();notificationPanel.revalidate();notificationPanel.repaint();
    }

    private boolean visibleToCurrent(AppNotification n){return currentUser.role().equals("Admin")||n.recipient().equalsIgnoreCase(currentUser.name());}
    private void saveNotifications(){try{notificationStore.save(notifications);}catch(Exception ex){System.err.println("FreshLink: could not save notification: "+ex.getMessage());}}
    private void notifyPerson(String recipient,String message){if(recipient==null||recipient.isBlank())return;notifications.add(new AppNotification(recipient,message));saveNotifications();if(settings.getBoolean("notificationSounds",false))Toolkit.getDefaultToolkit().beep();if(notificationButton!=null)notificationButton.setText("♧  "+notifications.stream().filter(this::visibleToCurrent).filter(n->!n.read()).count());}
    private void showNotifications(){refreshNotifications();}

    private JPanel buildUsersPage(){
        JPanel page=new JPanel(new BorderLayout(0,16));page.setOpaque(false);page.add(sectionHeading("Community accounts","Local Donor and Collector organizations using this workspace"),BorderLayout.NORTH);
        JPanel rows=new JPanel(new GridLayout(0,1,0,8));rows.setOpaque(false);for(User u:users){JPanel row=new Surface(new BorderLayout(14,0));row.setBorder(new EmptyBorder(13,17,13,17));row.add(label(u.name(),13,INK,Font.BOLD),BorderLayout.WEST);row.add(label(u.email(),11,MUTED,Font.PLAIN),BorderLayout.CENTER);JLabel role=label(u.role().toUpperCase(Locale.ROOT),9,u.role().equals("Admin")?new Color(124,58,237):GREEN,Font.BOLD);row.add(role,BorderLayout.EAST);rows.add(row);}
        JPanel outer=new JPanel(new BorderLayout());outer.setOpaque(false);outer.add(rows,BorderLayout.NORTH);page.add(outer,BorderLayout.CENTER);return page;
    }

    private JPanel buildSettingsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16)); page.setOpaque(false);
        page.add(sectionHeading("Workspace settings", "Local preferences and data tools"), BorderLayout.NORTH);
        JPanel card = new Surface(new GridBagLayout()); card.setBorder(new EmptyBorder(24, 28, 24, 28));
        GridBagConstraints c = new GridBagConstraints(); c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.WEST; c.insets=new Insets(9,8,9,20);
        card.add(label("Accent color",13,INK,Font.BOLD),c); c.gridx=1;
        JComboBox<String> accent = new JComboBox<>(new String[]{"Green", "Blue", "Orange", "Purple"}); accent.setSelectedItem(settings.get("accent","Green")); card.add(accent,c);
        accent.addActionListener(e->{String v=String.valueOf(accent.getSelectedItem());Color old=GREEN;GREEN=accentColor(v);settings.put("accent",v);refreshAccent(frame.getContentPane(),old,GREEN);frame.repaint();});
        c.gridy++; c.gridx=0; card.add(label("Text size",13,INK,Font.BOLD),c); c.gridx=1; JComboBox<String> fontSize=new JComboBox<>(new String[]{"Small","Medium","Large"});fontSize.setSelectedItem(settings.get("fontSize","Medium"));card.add(fontSize,c);fontSize.addActionListener(e->{String v=String.valueOf(fontSize.getSelectedItem());float old=fontScale;fontScale=switch(v){case "Small"->0.92f;case "Large"->1.12f;default->1f;};settings.put("fontSize",v);applyFontScale(frame.getContentPane(),fontScale/old);frame.revalidate();});
        c.gridy++; c.gridx=0; card.add(label("Default pickup city",13,INK,Font.BOLD),c); c.gridx=1; JTextField city=inputField("e.g. Bengaluru"); city.setText(settings.get("city","Community area")); city.setColumns(22); card.add(city,c);city.addActionListener(e->settings.put("city",city.getText().trim()));city.addFocusListener(new java.awt.event.FocusAdapter(){@Override public void focusLost(java.awt.event.FocusEvent e){settings.put("city",city.getText().trim());}});
        c.gridy++; c.gridx=0; card.add(label("Default pickup duration",13,INK,Font.BOLD),c); c.gridx=1; JComboBox<String> duration=new JComboBox<>(new String[]{"2 hours","4 hours","6 hours","8 hours"});duration.setSelectedItem(settings.get("pickupDuration","4 hours"));card.add(duration,c);duration.addActionListener(e->settings.put("pickupDuration",String.valueOf(duration.getSelectedItem())));
        c.gridy++; c.gridx=0; card.add(label("Expiry alerts for admins",13,INK,Font.BOLD),c); c.gridx=1; JCheckBox autoExpiry=new JCheckBox();autoExpiry.setOpaque(false);autoExpiry.setSelected(settings.getBoolean("autoExpiry",true));card.add(autoExpiry,c);autoExpiry.addActionListener(e->settings.put("autoExpiry",autoExpiry.isSelected()));
        c.gridy++; c.gridx=0; card.add(label("Notification sounds",13,INK,Font.BOLD),c); c.gridx=1; JCheckBox sound=new JCheckBox();sound.setOpaque(false);sound.setSelected(settings.getBoolean("notificationSounds",false));card.add(sound,c);sound.addActionListener(e->settings.put("notificationSounds",sound.isSelected()));
        c.gridy++; c.gridx=0; card.add(label("Inventory data",13,INK,Font.BOLD),c); c.gridx=1;
        JButton backup = secondaryButton("Restore a backup"); backup.addActionListener(e -> restoreBackup()); card.add(backup,c);
        c.gridy++; c.gridx=0; card.add(label("About FreshLink",13,INK,Font.BOLD),c); c.gridx=1;
        card.add(label("Local-first food rescue coordination · Version 1.0",12,MUTED,Font.PLAIN),c);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card,BorderLayout.NORTH); page.add(wrap,BorderLayout.CENTER); return page;
    }

    private JPanel sectionHeading(String title, String subtitle) {
        JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.add(label(title,19,INK,Font.BOLD)); p.add(Box.createVerticalStrut(5)); p.add(label(subtitle,12,MUTED,Font.PLAIN)); return p;
    }

    private JPanel chartCard(String title, JComponent chart) {
        JPanel p = new Surface(new BorderLayout(0, 10)); p.setBorder(new EmptyBorder(17,19,15,19));
        p.add(label(title,14,INK,Font.BOLD),BorderLayout.NORTH); p.add(chart,BorderLayout.CENTER); p.setPreferredSize(new Dimension(400,230)); return p;
    }

    private JPanel buildUrgentPanel() {
        JPanel p = new Surface(new BorderLayout(0,10)); p.setBorder(new EmptyBorder(17,19,16,19));
        p.add(label("Needs attention",14,INK,Font.BOLD),BorderLayout.NORTH);
        JPanel rows = new JPanel(); rows.setOpaque(false); rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));
        List<FoodItem> urgent = items.stream().filter(i->i.isUrgent()||i.expiresSoon()).sorted(Comparator.comparing(FoodItem::getExpiryDate)).limit(4).toList();
        if (urgent.isEmpty()) rows.add(label("No urgent listings right now. Check back as pickup windows approach.",12,MUTED,Font.PLAIN));
        for (FoodItem i : urgent) { JPanel r=new JPanel(new BorderLayout(8,0)); r.setOpaque(false); r.setBorder(new EmptyBorder(5,0,5,0));
            r.add(label("●  " + i.getName(),12, new Color(179,96,27),Font.BOLD),BorderLayout.WEST);
            r.add(label(i.getExpiryDate().format(DateTimeFormatter.ofPattern("MMM d"))+" · "+i.getLocation(),10,MUTED,Font.PLAIN),BorderLayout.EAST); rows.add(r); }
        p.add(rows,BorderLayout.CENTER); return p;
    }

    private JPanel buildActivityPanel() {
        JPanel p = new Surface(new BorderLayout(0,10)); p.setBorder(new EmptyBorder(17,19,16,19));
        p.add(label("Latest activity",14,INK,Font.BOLD),BorderLayout.NORTH);
        JPanel rows = new JPanel(); rows.setOpaque(false); rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));
        List<FoodItem> recent = items.stream().sorted(Comparator.comparing(FoodItem::getExpiryDate).reversed()).limit(4).toList();
        for (FoodItem i : recent) { String action = i.isClaimed() ? "was collected by " + i.getClaimedBy() : i.isReserved() ? "reserved by " + i.getReservedBy() : "is available for rescue";
            rows.add(label("•  " + i.getDonor() + " · " + i.getQuantity()+" "+i.getUnit()+" "+i.getName(),11,INK,Font.BOLD));
            rows.add(label("     " + action,10,MUTED,Font.PLAIN)); rows.add(Box.createVerticalStrut(5)); }
        p.add(rows,BorderLayout.CENTER); return p;
    }

    private JPanel reportMetric(String title, java.util.function.LongSupplier value, Color accent) {
        JPanel p=new Surface(new BorderLayout(0,10)); p.setBorder(new EmptyBorder(18,20,17,20)); p.add(label(title,10,MUTED,Font.BOLD),BorderLayout.NORTH);
        JLabel amount=label(Long.toString(value.getAsLong()),30,accent,Font.BOLD);reportValueLabels.add(amount);reportSuppliers.add(value);p.add(amount,BorderLayout.CENTER); return p;
    }

    private JPanel buildImpactByCategory() {
        JPanel p = chartCard("Inventory by category", new CategoryChart()); p.setPreferredSize(new Dimension(600,220)); return p;
    }

    private long estimatedMeals() {
        return items.stream().filter(FoodItem::isClaimed).mapToLong(i -> {
            double rate = switch(i.getCategory().toLowerCase(Locale.ROOT)) { case "prepared meals" -> 2.0; case "bakery" -> 1.0; case "produce" -> 1.5; case "dairy" -> 1.2; default -> 1.0; };
            return Math.round(i.getQuantity() * rate);
        }).sum();
    }

    private void exportReport() {
        JFileChooser chooser=new JFileChooser(); chooser.setSelectedFile(new java.io.File("freshlink-impact-report.csv"));
        if(chooser.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        try { List<String> rows=new ArrayList<>(); rows.add(CsvUtil.row("metric","value")); rows.add(CsvUtil.row("Listings created",Integer.toString(items.size())));
            rows.add(CsvUtil.row("Collected listings",Long.toString(items.stream().filter(FoodItem::isClaimed).count()))); rows.add(CsvUtil.row("Meals rescued estimate",Long.toString(estimatedMeals())));
            for(String cat:items.stream().map(FoodItem::getCategory).distinct().sorted().toList()) rows.add(CsvUtil.row("Quantity · "+cat,Long.toString(items.stream().filter(i->i.getCategory().equals(cat)&&i.isClaimed()).mapToLong(FoodItem::getQuantity).sum())));
            Files.write(chooser.getSelectedFile().toPath(),rows,StandardCharsets.UTF_8); toast("Impact report exported."); } catch(Exception ex){toast("Could not export report: "+ex.getMessage());}
    }

    private JPanel buildOverview() {
        JPanel wrap = new JPanel(); wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(buildHero()); wrap.add(Box.createVerticalStrut(17));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(label("TODAY AT A GLANCE", 10, MUTED, Font.BOLD), BorderLayout.WEST);
        top.add(label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), 11, MUTED, Font.PLAIN), BorderLayout.EAST);
        wrap.add(top); wrap.add(Box.createVerticalStrut(11));
        JPanel cards = new JPanel(new GridLayout(1, 5, 12, 0)); cards.setOpaque(false);
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);
        cards.add(metricCard("ACTIVE LISTINGS", activeValue, "Available and reserved", "▦", new Color(37, 111, 82)));
        cards.add(metricCard("AVAILABLE NOW", availableValue, "Ready for pickup", "⌂", new Color(22, 163, 74)));
        cards.add(metricCard("RESERVED", reservedValue, "Awaiting collection", "◷", new Color(124, 58, 237)));
        cards.add(metricCard("EXPIRING SOON", urgentValue, "Within 48 hours", "!", new Color(217, 119, 6)));
        cards.add(metricCard("MEALS RESCUED", rescuedValue, "Estimated portions", "✓", new Color(37, 99, 190)));
        wrap.add(cards);
        return wrap;
    }

    private JPanel buildHero() {
        JPanel hero = new JPanel(new BorderLayout(14, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(28, 88, 66)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 15)); g2.fillOval(getWidth() - 152, -61, 192, 192);
                g2.setColor(new Color(255, 255, 255, 12)); g2.fillOval(getWidth() - 100, 24, 88, 88);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hero.setOpaque(false); hero.setBorder(new EmptyBorder(18, 22, 18, 22));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 103)); hero.setPreferredSize(new Dimension(600, 103));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel eyebrow = label("GOOD FOOD, CIRCULATING", 9, new Color(191, 224, 204), Font.BOLD);
        JLabel headline = label("Give good food a second chance.", 20, Color.WHITE, Font.BOLD);
        JLabel subline = label("A little less waste. A little more good in the neighbourhood.", 11, new Color(222, 239, 227), Font.PLAIN);
        copy.add(eyebrow); copy.add(Box.createVerticalStrut(5)); copy.add(headline); copy.add(Box.createVerticalStrut(4)); copy.add(subline);
        hero.add(copy, BorderLayout.CENTER);
        JLabel mark = label("✳", 37, new Color(222, 239, 227), Font.BOLD);
        mark.setBorder(new EmptyBorder(0, 10, 0, 13)); hero.add(mark, BorderLayout.EAST);
        return hero;
    }

    private JPanel metricCard(String title, JLabel value, String caption, String icon, Color accent) {
        JPanel card = new Surface(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(17, 19, 17, 19));
        card.setMinimumSize(new Dimension(140, 120));
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        head.add(label(title, 9, MUTED, Font.BOLD), BorderLayout.WEST);
        JLabel badge = label(icon, 15, accent, Font.BOLD); head.add(badge, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);
        value.setFont(new Font("SansSerif", Font.BOLD, 28)); value.setForeground(INK);
        JPanel numbers = new JPanel(); numbers.setOpaque(false); numbers.setLayout(new BoxLayout(numbers, BoxLayout.Y_AXIS));
        numbers.add(value); numbers.add(Box.createVerticalStrut(3)); numbers.add(label(caption, 11, MUTED, Font.PLAIN));
        card.add(numbers, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildListings() {
        JPanel card = new Surface(new BorderLayout(0, 0));
        JPanel head = new JPanel(new BorderLayout(10, 10)); head.setOpaque(false);
        head.setBorder(new EmptyBorder(19, 21, 16, 21));
        JPanel ht = new JPanel(); ht.setOpaque(false); ht.setLayout(new BoxLayout(ht, BoxLayout.Y_AXIS));
        JLabel h = label("Food listings", 16, INK, Font.BOLD); ht.add(h); ht.add(Box.createVerticalStrut(4));
        ht.add(label("Browse available donations and their pickup details.", 11, MUTED, Font.PLAIN));
        head.add(ht, BorderLayout.WEST);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0)); tools.setOpaque(false);
        search.setPreferredSize(new Dimension(200, 35));
        search.setFont(new Font("SansSerif", Font.PLAIN, 12));
        search.setForeground(INK); search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE), new EmptyBorder(0, 10, 0, 10)));
        search.putClientProperty("JTextField.placeholderText", "⌕  Search food, donor...");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshTable(); }
        });
        tools.add(search);
        JComboBox<String> sort = new JComboBox<>(new String[]{"Soonest expiry", "Latest expiry", "Name A–Z", "Quantity", "Pickup location"});
        sort.setFont(new Font("SansSerif", Font.PLAIN, 11)); sort.setBackground(Color.WHITE);
        sort.addActionListener(e -> { model.sortMode = sort.getSelectedIndex(); refreshTable(); });
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All listings", "Available", "Reserved", "Collected", "Expired", "Expiring soon"});
        statusFilter.setFont(new Font("SansSerif", Font.PLAIN, 11)); statusFilter.setBackground(Color.WHITE);
        statusFilter.addActionListener(e -> { activeFilter=String.valueOf(statusFilter.getSelectedItem()); refreshTable(); });
        JComboBox<String> categoryFilter=new JComboBox<>(new String[]{"All categories","Bakery","Produce","Dairy","Prepared meals","Pantry","Beverage","Frozen","Other"});categoryFilter.setFont(new Font("SansSerif",Font.PLAIN,11));categoryFilter.setBackground(Color.WHITE);categoryFilter.addActionListener(e->{selectedCategory=String.valueOf(categoryFilter.getSelectedItem());refreshTable();});
        JButton cardMode=textButton("Cards"); cardMode.addActionListener(e->{viewMode="Cards";listingModeLayout.show(listingModes,"Cards");});
        JButton tableMode=textButton("Table"); tableMode.addActionListener(e->{viewMode="Table";listingModeLayout.show(listingModes,"Table");});
        tools.add(statusFilter); tools.add(categoryFilter); tools.add(sort); tools.add(cardMode); tools.add(tableMode); head.add(tools, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        table.setRowHeight(58); table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(INK); table.setBackground(Color.WHITE); table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true); table.setGridColor(new Color(241, 243, 241));
        table.setIntercellSpacing(new Dimension(0, 0)); table.setSelectionBackground(new Color(238, 246, 240));
        table.setSelectionForeground(INK); table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 37));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 9));
        table.getTableHeader().setForeground(MUTED); table.getTableHeader().setBackground(new Color(250, 251, 249));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, LINE));
        table.setDefaultRenderer(Object.class, new ListingRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(1).setPreferredWidth(115);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);
        table.getColumnModel().getColumn(3).setPreferredWidth(105);
        table.getColumnModel().getColumn(4).setPreferredWidth(125);
        table.getColumnModel().getColumn(5).setPreferredWidth(105);
        JScrollPane scroll = new JScrollPane(table) {
            @Override protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                if (model.getRowCount() != 0) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                g2.setColor(new Color(237, 245, 239)); g2.fillOval(cx - 24, getHeight() / 2 - 54, 48, 48);
                g2.setColor(GREEN); g2.setFont(new Font("SansSerif", Font.BOLD, 25));
                g2.drawString("✳", cx - 11, getHeight() / 2 - 21);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14)); g2.setColor(INK);
                String title = "Your rescue board is ready";
                g2.drawString(title, cx - g2.getFontMetrics().stringWidth(title) / 2, getHeight() / 2 + 19);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11)); g2.setColor(MUTED);
                String detail = "Add the first surplus food listing to get started.";
                g2.drawString(detail, cx - g2.getFontMetrics().stringWidth(detail) / 2, getHeight() / 2 + 42);
                g2.dispose();
            }
        };
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getViewport().setBackground(Color.WHITE);
        listingCards.setBorder(new EmptyBorder(10,10,14,10)); listingCards.setOpaque(false);
        JScrollPane cardScroll=new JScrollPane(listingCards); cardScroll.setBorder(BorderFactory.createEmptyBorder()); cardScroll.getViewport().setOpaque(false); cardScroll.setOpaque(false);
        listingModes.add(scroll,"Table"); listingModes.add(cardScroll,"Cards"); listingModeLayout.show(listingModes,viewMode);
        JPanel middle=new JPanel(new BorderLayout()); middle.setOpaque(false); middle.add(listingModes,BorderLayout.CENTER);
        card.add(middle, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 21, 12, 21));
        footer.add(tableCount, BorderLayout.WEST); tableCount.setFont(new Font("SansSerif", Font.PLAIN, 10)); tableCount.setForeground(MUTED);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        if(currentUser.role().equals("Collector")||currentUser.role().equals("Admin")){JButton reserve=secondaryButton("Reserve");reserve.addActionListener(e->reserveSelected());actions.add(reserve);JButton claim=secondaryButton("Mark collected");claim.addActionListener(e->markCollected());actions.add(claim);}
        JButton cancel = textButton("Cancel reservation"); cancel.addActionListener(e -> cancelReservation());
        JButton delete = textButton("Remove"); delete.addActionListener(e -> removeSelected());
        JButton edit = textButton("Edit listing"); edit.addActionListener(e -> editSelected());
        JButton export = textButton("Export CSV"); export.addActionListener(e -> exportInventory());
        actions.add(cancel); if(!currentUser.role().equals("Collector")){actions.add(edit);actions.add(delete);} actions.add(export);
        if (currentUser.role().equals("Admin")) { JButton restore = textButton("Restore backup"); restore.addActionListener(e -> restoreBackup()); actions.add(restore); }
        footer.add(actions, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private void refresh() { refreshCards(); refreshTable(); updateNav(); for(int i=0;i<reportValueLabels.size();i++)reportValueLabels.get(i).setText(Long.toString(reportSuppliers.get(i).getAsLong())); if(notificationButton!=null)notificationButton.setText("♧  "+notifications.stream().filter(this::visibleToCurrent).filter(n->!n.read()).count()); }

    private void refreshCards() {
        long available = items.stream().filter(i -> !i.isClaimed() && !i.isReserved() && !i.isExpired()).count();
        long urgent = items.stream().filter(FoodItem::expiresSoon).count();
        long reserved = items.stream().filter(FoodItem::isReserved).count();
        long active = items.stream().filter(i -> !i.isExpired() && !i.isClaimed()).count();
        availableValue.setText(Long.toString(available)); urgentValue.setText(Long.toString(urgent)); reservedValue.setText(Long.toString(reserved)); activeValue.setText(Long.toString(active)); rescuedValue.setText(Long.toString(estimatedMeals()));
    }

    private void refreshTable() {
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        List<FoodItem> visible = items.stream().filter(i -> switch (activeFilter) {
                    case "Expiring soon" -> i.expiresSoon();
                    case "Rescued" -> i.isClaimed();
                    case "Available" -> !i.isClaimed() && !i.isReserved() && !i.isExpired();
                    case "Reserved" -> i.isReserved();
                    case "Collected" -> i.isClaimed();
                    case "Expired" -> i.isExpired() && !i.isClaimed();
                    case "Urgent rescue" -> !i.isClaimed() && (i.isUrgent() || i.expiresSoon() || i.isExpired());
                    case "My reservations" -> currentUser.role().equals("Collector") ? i.getReservedBy().equals(currentUser.name()) : i.getDonor().equalsIgnoreCase(currentUser.name());
                    default -> true;
                }).filter(i -> selectedCategory.equals("All categories") || i.getCategory().equalsIgnoreCase(selectedCategory))
                .filter(i -> q.isEmpty() || (i.getName() + " " + i.getCategory() + " " + i.getDonor() + " " + i.getLocation()+" "+i.getNotes()+" "+(i.isClaimed()?"collected":i.isReserved()?"reserved":i.isExpired()?"expired":"available"))
                        .toLowerCase(Locale.ROOT).contains(q))
                .sorted(switch(model.sortMode){case 1->Comparator.comparing(FoodItem::getExpiryDate).reversed();case 2->Comparator.comparing(i->i.getName().toLowerCase(Locale.ROOT));case 3->Comparator.comparingInt(FoodItem::getQuantity).reversed();case 4->Comparator.comparing(i->i.getLocation().toLowerCase(Locale.ROOT));default->Comparator.comparing(FoodItem::getExpiryDate);})
                .collect(Collectors.toList());
        model.setRows(visible);
        tableCount.setText("Showing "+visible.size()+" of "+items.size()+" listings");
        rebuildCards(visible); pageHost.repaint();
    }

    private void rebuildCards(List<FoodItem> visible) {
        if (listingCards == null) return;
        listingCards.removeAll();
        if(visible.isEmpty()){JPanel empty=new JPanel(new GridBagLayout());empty.setOpaque(false);empty.add(label("No listings match these filters. Try another search or status.",13,MUTED,Font.PLAIN));listingCards.add(empty);}
        for (FoodItem item : visible) listingCards.add(foodCard(item));
        listingCards.revalidate(); listingCards.repaint();
    }

    private JPanel foodCard(FoodItem item) {
        Color category = categoryColor(item.getCategory());
        JPanel card = new Surface(new BorderLayout(0,9)); card.setBorder(new EmptyBorder(15,16,13,16));
        JPanel top=new JPanel(new BorderLayout(8,0)); top.setOpaque(false);
        JLabel icon=label(categoryGlyph(item.getCategory()),19,category,Font.BOLD); top.add(icon,BorderLayout.WEST);
        String state=item.isClaimed()?"COLLECTED":item.isReserved()?"RESERVED":item.isExpired()?"EXPIRED":item.isUrgent()?"URGENT RESCUE":item.expiresSoon()?"EXPIRING SOON":"AVAILABLE";
        JLabel pill=label("  "+state+"  ",9, statusColor(item),Font.BOLD); pill.setOpaque(true); pill.setBackground(new Color(statusColor(item).getRed(),statusColor(item).getGreen(),statusColor(item).getBlue(),24)); top.add(pill,BorderLayout.EAST);
        card.add(top,BorderLayout.NORTH);
        JPanel copy=new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy,BoxLayout.Y_AXIS));
        copy.add(label(item.getName(),14,INK,Font.BOLD)); copy.add(Box.createVerticalStrut(4));
        copy.add(label(item.getQuantity()+" "+item.getUnit()+"  ·  "+item.getCategory(),11,category,Font.BOLD));
        copy.add(Box.createVerticalStrut(9)); copy.add(label("DONOR",9,MUTED,Font.BOLD)); copy.add(label(item.getDonor(),11,INK,Font.PLAIN));
        copy.add(Box.createVerticalStrut(5)); copy.add(label("PICKUP",9,MUTED,Font.BOLD)); copy.add(label(item.getLocation(),11,INK,Font.PLAIN));
        copy.add(Box.createVerticalStrut(5));String window=item.getPickupFrom()==null?"Pickup time by arrangement":item.getPickupFrom()+"–"+item.getPickupUntil();copy.add(label("Pickup window  "+window,10,MUTED,Font.PLAIN));
        if(!item.getContactNumber().isBlank())copy.add(label("Contact  "+item.getContactNumber(),10,MUTED,Font.PLAIN));
        copy.add(Box.createVerticalStrut(6)); copy.add(label("Best before  "+item.getExpiryDate().format(DATE)+(item.getPriority().equalsIgnoreCase("Urgent")?"  ·  URGENT":""),10,statusColor(item),Font.BOLD));
        if(!item.getNotes().isBlank()) { copy.add(Box.createVerticalStrut(5)); JLabel note=label(item.getNotes(),10,MUTED,Font.PLAIN); copy.add(note); }
        card.add(copy,BorderLayout.CENTER);
        JButton quick=secondaryButton(item.isReserved()?"Reservation details":"View listing"); quick.addActionListener(e->{table.clearSelection(); for(int r=0;r<model.rows.size();r++)if(model.rows.get(r).getId().equals(item.getId())){table.setRowSelectionInterval(r,r); break;} });
        card.add(quick,BorderLayout.SOUTH); card.setPreferredSize(new Dimension(240,235)); return card;
    }

    private Color categoryColor(String category) {
        return switch(category.toLowerCase(Locale.ROOT)){case "bakery"->new Color(194,126,40);case "produce"->new Color(37,133,81);case "dairy"->new Color(37,99,190);case "prepared meals"->new Color(221,103,39);case "pantry"->new Color(124,58,237);case "beverage"->new Color(8,145,178);case "frozen"->new Color(14,116,144);default->new Color(100,116,139);};
    }
    private String categoryGlyph(String category){return switch(category.toLowerCase(Locale.ROOT)){case "bakery"->"♨";case "produce"->"✿";case "dairy"->"◉";case "prepared meals"->"♨";case "pantry"->"▦";case "beverage"->"◒";case "frozen"->"❄";default->"●";};}
    private Color statusColor(FoodItem i){return i.isExpired()?new Color(100,116,139):i.isClaimed()?new Color(37,99,190):i.isReserved()?new Color(124,58,237):i.isUrgent()||i.expiresSoon()?new Color(217,119,6):GREEN;}

    private void updateNav() {
        // Rebuild nav states while keeping the sidebar layout compact.
        for (Component c : ((JPanel) frame.getContentPane().getComponent(0)).getComponents()) {
            if (c instanceof JButton b) {
                String t = b.getText();
                boolean selected = activeFilter.equals("My reservations") ? t.contains("My reservations")
                        : activeFilter.equals("Urgent rescue") ? t.contains("Urgent rescue")
                        : t.contains(activePage);
                b.setForeground(selected ? GREEN : new Color(84, 99, 90));
                b.setBackground(selected ? new Color(237, 245, 239) : Color.WHITE);
            }
        }
    }

    private void showAddDialog() {
        if (!(currentUser.role().equals("Donor") || currentUser.role().equals("Admin"))) { toast("Only donors and admins can add listings."); return; }
        showListingDialog(null);
    }

    private void editSelected() {
        FoodItem item = selectedItem(); if (item == null) { toast("Select a listing to edit."); return; }
        boolean allowed = currentUser.role().equals("Admin") || currentUser.role().equals("Donor") && item.getDonor().equalsIgnoreCase(currentUser.name());
        if (!allowed) { toast("You can edit only your own listings."); return; }
        showListingDialog(item);
    }

    private void showListingDialog(FoodItem existing) {
        JTextField name = inputField("e.g. Fresh sourdough loaves");
        JComboBox<String> category = new JComboBox<>(new String[]{"Bakery", "Produce", "Dairy", "Pantry", "Prepared meals", "Beverage", "Frozen", "Other"});
        category.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        JComboBox<String> unit = new JComboBox<>(new String[]{"pieces", "kg", "litres", "packets", "trays", "boxes", "portions", "bags", "items"});
        JTextField expiry = inputField(LocalDate.now().plusDays(2).toString()); expiry.setText(LocalDate.now().plusDays(2).toString());
        JTextField donor = inputField("e.g. Green Corner Market");
        JTextField contact = inputField("e.g. +91 98765 43210");
        JTextField location = inputField("e.g. 24 Market Street");location.setText(settings.get("city",""));
        JTextField pickupFrom = inputField("14:00"); pickupFrom.setText("14:00");
        int hours=4;try{hours=Integer.parseInt(settings.get("pickupDuration","4 hours").split(" ")[0]);}catch(Exception ignored){}
        JTextField pickupUntil = inputField("18:00"); pickupUntil.setText(java.time.LocalTime.of(14,0).plusHours(hours).toString());
        JCheckBox urgent = new JCheckBox("Mark this listing as urgent rescue"); urgent.setOpaque(false); urgent.setForeground(new Color(194,82,34));
        JTextArea notes = new JTextArea(3, 25); notes.setFont(new Font("SansSerif", Font.PLAIN, 12)); notes.setLineWrap(true); notes.setWrapStyleWord(true);
        if (existing != null) {
            name.setText(existing.getName()); category.setSelectedItem(existing.getCategory()); quantity.setValue(existing.getQuantity()); unit.setSelectedItem(existing.getUnit());
            expiry.setText(existing.getExpiryDate().toString()); donor.setText(existing.getDonor()); location.setText(existing.getLocation()); notes.setText(existing.getNotes());contact.setText(existing.getContactNumber());
            if(existing.getPickupFrom()!=null)pickupFrom.setText(existing.getPickupFrom().toString());if(existing.getPickupUntil()!=null)pickupUntil.setText(existing.getPickupUntil().toString());urgent.setSelected(existing.getPriority().equalsIgnoreCase("Urgent"));
        }
        JPanel form = new JPanel(new GridBagLayout()); form.setBackground(Color.WHITE); form.setBorder(new EmptyBorder(8, 4, 4, 4));
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(6, 5, 6, 5); c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0; c.gridx = 0; c.gridy = 0;
        addFormRow(form, c, "Food name", name); addFormRow(form, c, "Category", category);
        JPanel qty = new JPanel(new GridLayout(1, 2, 8, 0)); qty.setOpaque(false); qty.add(quantity); qty.add(unit);
        addFormRow(form, c, "Quantity", qty); addFormRow(form, c, "Best before (YYYY-MM-DD)", expiry);
        if (currentUser.role().equals("Donor")) { donor.setText(existing == null ? currentUser.name() : existing.getDonor()); donor.setEditable(false); }
        addFormRow(form, c, "Donated by", donor); addFormRow(form,c,"Donor contact",contact);addFormRow(form, c, "Pickup location", location);
        JPanel timeRange=new JPanel(new GridLayout(1,2,8,0));timeRange.setOpaque(false);timeRange.add(pickupFrom);timeRange.add(pickupUntil);addFormRow(form,c,"Pickup window (from / until)",timeRange);addFormRow(form,c,"Rescue priority",urgent);
        JScrollPane noteScroll = new JScrollPane(notes); noteScroll.setBorder(BorderFactory.createLineBorder(LINE));
        addFormRow(form, c, "Pickup notes", noteScroll);
        int result = JOptionPane.showConfirmDialog(frame, form, existing == null ? "Add a food listing" : "Edit food listing", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        if (name.getText().isBlank() || donor.getText().isBlank() || location.getText().isBlank()) {
            toast("Add a food name, donor, and pickup location to continue."); return;
        }
        LocalDate date;
        try { date = LocalDate.parse(expiry.getText().trim()); }
        catch (DateTimeParseException ex) { toast("Enter the date as YYYY-MM-DD."); return; }
        if (date.isBefore(LocalDate.now())) { toast("The best-before date must be today or later."); return; }
        String phoneDigits=contact.getText().replaceAll("\\D","");if(!contact.getText().isBlank()&&(!contact.getText().trim().matches("\\+?[0-9\\s-]+")||phoneDigits.length()<7||phoneDigits.length()>15)){toast("Enter a valid phone number with 7–15 digits.");return;}
        java.time.LocalTime from,until;try{from=java.time.LocalTime.parse(pickupFrom.getText().trim());until=java.time.LocalTime.parse(pickupUntil.getText().trim());if(!until.isAfter(from))throw new DateTimeParseException("Pickup end must be later",pickupUntil.getText(),0);}
        catch(DateTimeParseException ex){toast("Enter pickup times as HH:mm and make the end time later than the start.");return;}
        FoodItem updated = existing == null ? FoodItem.create(name.getText().trim(), (String) category.getSelectedItem(),
                (Integer) quantity.getValue(), (String) unit.getSelectedItem(), date, donor.getText().trim(), location.getText().trim(), notes.getText().trim(),contact.getText().trim(),from,until,urgent.isSelected()?"Urgent":"Normal")
                : new FoodItem(existing.getId(), name.getText().trim(), (String) category.getSelectedItem(), (Integer) quantity.getValue(), (String) unit.getSelectedItem(), date,
                donor.getText().trim(), location.getText().trim(), notes.getText().trim(), existing.getClaimedBy(), existing.getReservedBy(),contact.getText().trim(),from,until,urgent.isSelected()?"Urgent":"Normal");
        if (existing == null) items.add(updated); else replace(existing, updated);
        persist();
    }

    private JTextField inputField(String hint) {
        JTextField f = new JTextField(24); f.setFont(new Font("SansSerif", Font.PLAIN, 12));
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(8, 9, 8, 9)));
        f.setToolTipText(hint); return f;
    }

    private void addFormRow(JPanel panel, GridBagConstraints c, String title, Component field) {
        c.gridx = 0; c.weightx = 0; c.gridy++; JLabel l = label(title, 11, INK, Font.BOLD); panel.add(l, c);
        c.gridx = 1; c.weightx = 1; panel.add(field, c);
    }

    private void markCollected() {
        if (!currentUser.role().equals("Collector") && !currentUser.role().equals("Admin")) { toast("Collectors can mark reserved food as collected."); return; }
        FoodItem item = selectedItem(); if (item == null) { toast("Select a listing to mark it collected."); return; }
        if (item.isClaimed() || !item.isReserved()) { toast("Reserve an available listing before collection."); return; }
        if (!currentUser.role().equals("Admin") && !item.getReservedBy().equals(currentUser.name())) { toast("Only the collector who reserved this listing can collect it."); return; }
        String who = JOptionPane.showInputDialog(frame, "Who collected this food?", "Record a rescue", JOptionPane.PLAIN_MESSAGE);
        if (who == null) return;
        if (who.isBlank()) { toast("Enter a name or group to record the collection."); return; }
        replace(item, item.claimedBy(who.trim())); notifyPerson(item.getDonor(),item.getName()+" was collected by "+who.trim()+"."); persist();
    }

    private void reserveSelected() {
        if (!currentUser.role().equals("Collector")) { toast("Only collectors can reserve listings."); return; }
        FoodItem item = selectedItem(); if (item == null) { toast("Select an available listing to reserve."); return; }
        if (item.isExpired() || item.isClaimed() || item.isReserved()) { toast("This listing is no longer available to reserve."); return; }
        replace(item, item.reservedBy(currentUser.name())); notifyPerson(item.getDonor(),item.getName()+" was reserved by "+currentUser.name()+"."); persist();
    }

    private void cancelReservation() {
        FoodItem item = selectedItem(); if (item == null || !item.isReserved()) { toast("Select a reserved listing first."); return; }
        boolean allowed = currentUser.role().equals("Admin") || currentUser.role().equals("Collector") && item.getReservedBy().equals(currentUser.name())
                || currentUser.role().equals("Donor") && item.getDonor().equalsIgnoreCase(currentUser.name());
        if (!allowed) { toast("Only the reserving collector, listing donor, or admin can cancel this reservation."); return; }
        replace(item, item.reservedBy("")); notifyPerson(item.getReservedBy(),"Reservation for "+item.getName()+" was cancelled by "+currentUser.name()+"."); persist();
    }

    private void removeSelected() {
        FoodItem item = selectedItem(); if (item == null) { toast("Select a listing to remove."); return; }
        boolean allowed = currentUser.role().equals("Admin") || currentUser.role().equals("Donor") && item.getDonor().equalsIgnoreCase(currentUser.name());
        if (!allowed) { toast("You can remove only your own listings."); return; }
        int choice = JOptionPane.showConfirmDialog(frame, "Remove “" + item.getName() + "” from FreshLink?",
                "Remove listing", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) { items.removeIf(i -> i.getId().equals(item.getId())); persist(); }
    }

    private FoodItem selectedItem() {
        int row = table.getSelectedRow(); if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row); return model.rows.get(modelRow);
    }

    private void replace(FoodItem before, FoodItem after) {
        for (int i = 0; i < items.size(); i++) if (items.get(i).getId().equals(before.getId())) { items.set(i, after); return; }
    }

    private void persist() {
        try { store.save(items); refresh(); toast("Saved. Your food rescue board is up to date."); }
        catch (Exception ex) { toast("Could not save the listing. Check available disk space and permissions."); }
    }

    private void exportInventory() {
        JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new java.io.File("freshlink-inventory.csv"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            List<String> rows = new ArrayList<>(); rows.add(CsvUtil.row("id","food","category","quantity","unit","bestBefore","donor","contactNumber","location","pickupFrom","pickupUntil","priority","notes","status","reservedBy","collectedBy"));
            for (FoodItem i : items) rows.add(CsvUtil.row(i.getId(),i.getName(),i.getCategory(),Integer.toString(i.getQuantity()),i.getUnit(),i.getExpiryDate().toString(),i.getDonor(),i.getContactNumber(),i.getLocation(),i.getPickupFrom()==null?"":i.getPickupFrom().toString(),i.getPickupUntil()==null?"":i.getPickupUntil().toString(),i.getPriority(),i.getNotes(),i.isClaimed()?"COLLECTED":i.isReserved()?"RESERVED":i.isExpired()?"EXPIRED":"AVAILABLE",i.getReservedBy(),i.getClaimedBy()));
            Files.write(chooser.getSelectedFile().toPath(), rows, StandardCharsets.UTF_8); toast("Inventory exported.");
        } catch (Exception ex) { toast("Could not export the inventory: " + ex.getMessage()); }
    }

    private void restoreBackup() {
        if (!currentUser.role().equals("Admin")) return;
        try {
            List<java.nio.file.Path> choices = store.backups(); if (choices.isEmpty()) { toast("No inventory backups are available yet."); return; }
            JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Choose a FreshLink backup");
            chooser.setCurrentDirectory(choices.get(0).getParent().toFile());
            if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
            int ok = JOptionPane.showConfirmDialog(frame, "Replace the current inventory with this backup?", "Restore backup", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) { store.restore(chooser.getSelectedFile().toPath()); items.clear(); items.addAll(store.load()); refresh(); toast("Backup restored."); }
        } catch (Exception ex) { toast("Could not restore backup: " + ex.getMessage()); }
    }

    private void toast(String message) {
        JOptionPane.showMessageDialog(frame, message, "FreshLink", JOptionPane.INFORMATION_MESSAGE);
    }

    private void installKeys() {
        frame.getRootPane().registerKeyboardAction(e -> showAddDialog(),
                KeyStroke.getKeyStroke("control N"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.getRootPane().registerKeyboardAction(e -> {activePage="Food board";pageTitle.setText("Food board");pageLayout.show(pageHost,"Food board");search.requestFocusInWindow();updateNav();},
                KeyStroke.getKeyStroke("control F"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static JLabel label(String text, int size, Color color, int style) {
        JLabel l = new JLabel(text); l.setFont(new Font("SansSerif", style, Math.max(9,Math.round(size*fontScale)))); l.setForeground(color); return l;
    }

    private static Color accentColor(String value){return switch(value){case "Blue"->new Color(37,99,235);case "Orange"->new Color(234,88,12);case "Purple"->new Color(124,58,237);default->new Color(22,163,74);};}
    private void refreshAccent(Component component,Color old,Color value){if(component.getForeground()!=null&&component.getForeground().equals(old))component.setForeground(value);if(component.getBackground()!=null&&component.getBackground().equals(old))component.setBackground(value);if(component instanceof Container container)for(Component child:container.getComponents())refreshAccent(child,old,value);}
    private void applyFontScale(Component component,float factor){Font font=component.getFont();if(font!=null)component.setFont(font.deriveFont(Math.max(9f,font.getSize2D()*factor)));if(component instanceof Container container)for(Component child:container.getComponents())applyFontScale(child,factor);}

    private static JButton primaryButton(String text) {
        JButton b = new JButton(text); b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(Color.WHITE); b.setBackground(GREEN); b.setOpaque(true); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setBorder(new EmptyBorder(12, 17, 12, 17)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton secondaryButton(String text) {
        JButton b = new JButton(text); b.setFont(new Font("SansSerif", Font.BOLD, 11)); b.setForeground(GREEN);
        b.setBackground(new Color(237, 245, 239)); b.setOpaque(true); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 12, 8, 12)); return b;
    }

    private static JButton textButton(String text) {
        JButton b = new JButton(text); b.setFont(new Font("SansSerif", Font.PLAIN, 11)); b.setForeground(MUTED);
        b.setContentAreaFilled(false); b.setBorder(new EmptyBorder(8, 8, 8, 8)); b.setFocusPainted(false); return b;
    }

    /** Rounded card surface with a low contrast shadow, painted with core Swing. */
    private static final class Surface extends JPanel {
        Surface(LayoutManager layout) { super(layout); setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(20,40,30,13)); g.fillRoundRect(2,4,getWidth()-4,getHeight()-6,18,18);
            g.setColor(Color.WHITE); g.fillRoundRect(0,0,getWidth()-4,getHeight()-7,18,18); g.dispose(); super.paintComponent(graphics);
        }
    }

    private final class CategoryChart extends JPanel {
        CategoryChart(){setOpaque(false);setPreferredSize(new Dimension(330,155));}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            java.util.Map<String,Long> counts=items.stream().collect(java.util.stream.Collectors.groupingBy(FoodItem::getCategory,java.util.LinkedHashMap::new,java.util.stream.Collectors.counting())); long total=counts.values().stream().mapToLong(Long::longValue).sum();
            int x=12,y=17,size=Math.min(130,getHeight()-28),start=90; if(total>0){for(var e:counts.entrySet()){int extent=(int)Math.round(360.0*e.getValue()/total);g.setColor(categoryColor(e.getKey()));g.fillArc(x,y,size,size,start,extent);start+=extent;}}
            g.setColor(Color.WHITE);g.fillOval(x+32,y+32,size-64,size-64);g.setColor(INK);g.setFont(new Font("SansSerif",Font.BOLD,18));g.drawString(Long.toString(total),x+size/2-7,y+size/2+3);g.setFont(new Font("SansSerif",Font.PLAIN,9));g.setColor(MUTED);g.drawString("listings",x+size/2-19,y+size/2+17);
            int lx=x+size+17,ly=27;g.setFont(new Font("SansSerif",Font.PLAIN,10));for(var e:counts.entrySet()){g.setColor(categoryColor(e.getKey()));g.fillRoundRect(lx,ly-8,8,8,3,3);g.setColor(INK);g.drawString(e.getKey()+"  "+e.getValue(),lx+14,ly);ly+=18;if(ly>getHeight()-8)break;}g.dispose();}
    }

    private final class StatusChart extends JPanel {
        StatusChart(){setOpaque(false);setPreferredSize(new Dimension(330,155));}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            String[] names={"Available","Reserved","Collected","Expired"};Color[] colors={GREEN,new Color(124,58,237),new Color(37,99,190),new Color(100,116,139)};
            long[] values={items.stream().filter(i->!i.isClaimed()&&!i.isReserved()&&!i.isExpired()).count(),items.stream().filter(FoodItem::isReserved).count(),items.stream().filter(FoodItem::isClaimed).count(),items.stream().filter(FoodItem::isExpired).count()};
            int max=(int)Math.max(1,java.util.Arrays.stream(values).max().orElse(1)),base=getHeight()-29,barW=Math.min(42,(getWidth()-55)/4),gap=22,left=24;g.setColor(new Color(235,239,236));g.drawLine(10,base,getWidth()-8,base);
            for(int i=0;i<4;i++){int h=(int)((base-30)*values[i]/max);int bx=left+i*(barW+gap);g.setColor(colors[i]);g.fillRoundRect(bx,base-h,barW,h,9,9);g.setColor(INK);g.setFont(new Font("SansSerif",Font.BOLD,10));g.drawString(Long.toString(values[i]),bx+barW/2-3,base-h-5);g.setColor(MUTED);g.setFont(new Font("SansSerif",Font.PLAIN,9));g.drawString(names[i],bx-2,base+14);}g.dispose();}
    }

    private final class ListingModel extends AbstractTableModel {
        private final String[] columns = {"FOOD & DONOR", "QUANTITY", "BEST BEFORE", "PICKUP", "STATUS", "COLLECTED BY"};
        private List<FoodItem> rows = new ArrayList<>();
        private int sortMode;
        void setRows(List<FoodItem> value) { rows = value; fireTableDataChanged(); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }
        @Override public Object getValueAt(int row, int col) {
            FoodItem i = rows.get(row);
            return switch (col) {
                case 0 -> i.getName() + "\n" + i.getCategory() + "  ·  " + i.getDonor();
                case 1 -> i.getQuantity() + " " + i.getUnit();
                case 2 -> i.getExpiryDate().format(DATE);
                case 3 -> i.getLocation();
                case 4 -> i.isClaimed() ? "Collected" : i.isExpired() ? "Expired" : i.isReserved() ? "Reserved" : i.expiresSoon() ? "Expiring soon" : "Available";
                default -> i.isClaimed() ? i.getClaimedBy() : "—";
            };
        }
    }

    private final class ListingRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                 boolean focus, int row, int col) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            cell.setBorder(new EmptyBorder(6, col == 0 ? 16 : 7, 6, 7)); cell.setFont(new Font("SansSerif", Font.PLAIN, 11));
            cell.setForeground(selected ? INK : new Color(74, 88, 79));
            if (col == 0) {
                String[] lines = value.toString().split("\n", 2);
                cell.setText("<html><b style='font-size:12px;color:#1f302a'>" + escape(lines[0]) +
                        "</b><br><span style='color:#87938b;font-size:10px'>" + escape(lines.length > 1 ? lines[1] : "") + "</span></html>");
            } else if (col == 4) {
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                Color bg = value.equals("Collected") ? new Color(232, 241, 251)
                        : value.equals("Expiring soon") || value.equals("Expired") ? new Color(252, 241, 224)
                        : new Color(236, 245, 238);
                Color fg = value.equals("Collected") ? new Color(56, 102, 162)
                        : value.equals("Expiring soon") || value.equals("Expired") ? new Color(161, 105, 30) : GREEN;
                cell.setOpaque(true); cell.setBackground(selected ? new Color(238, 246, 240) : bg); cell.setForeground(fg);
                cell.setFont(new Font("SansSerif", Font.BOLD, 10));
                cell.setBorder(new EmptyBorder(5, 4, 5, 4));
            } else if (!selected) cell.setBackground(Color.WHITE);
            return cell;
        }
        private String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    }
}
