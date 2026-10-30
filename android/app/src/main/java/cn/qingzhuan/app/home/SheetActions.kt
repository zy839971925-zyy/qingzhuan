package cn.qingzhuan.app

/** Retained outgoing pages may draw, but their old callbacks must never mutate live app state. */
fun HomeActions.whenActive(active: () -> Boolean): HomeActions = HomeActions(
    pick = { if (active()) pick() },
    pickPhotos = { if (active()) pickPhotos() },
    convert = { id, tool, options, ids -> active() && convert(id, tool, options, ids) },
    cancel = { if (active()) cancel(it) },
    save = { if (active()) save(it) },
    share = { if (active()) share(it) },
    open = { if (active()) open(it) },
    removeInputs = { if (active()) removeInputs(it) },
    removeOutputs = { if (active()) removeOutputs(it) },
    setTheme = { if (active()) setTheme(it) },
    readLicenses = readLicenses,
    haptic = { if (active()) haptic() },
    notice = { if (active()) notice(it) },
    preview = preview,
    pickBackground = { if (active()) pickBackground() },
    pickBackgroundPhotos = { if (active()) pickBackgroundPhotos() },
    setBackground = { mode, blur -> if (active()) setBackground(mode, blur) },
    backgroundFile = backgroundFile,
    acknowledge = { if (active()) acknowledge(it) },
    openInput = { if (active()) openInput(it) },
    setBackgroundStyle = { if (active()) setBackgroundStyle(it) },
    languageMode = languageMode,
    setLanguage = { if (active()) setLanguage(it) },
)
