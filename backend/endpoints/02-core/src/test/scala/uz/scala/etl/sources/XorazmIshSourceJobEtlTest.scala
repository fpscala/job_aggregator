package uz.scala.etl.sources

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

import weaver.SimpleIOSuite

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails
import uz.scala.repos.dto

object XorazmIshSourceJobEtlTest extends SimpleIOSuite {
  private val postedAt =
    ZonedDateTime.of(2026, 3, 1, 10, 0, 0, 0, ZoneId.of("Asia/Samarkand"))

  pureTest("filters source channel and keeps hidden application links") {
    val rawJob =
      RawJob(
        title = "operator",
        company = Some("Test kompaniyasi"),
        description =
          """#ish
            |Test kompaniyasiga operator ishga taklif qilinadi.
            |
            |Ariza topshirish:
            |👤 Onlayn anketa
            |
            |👉 @Xorazm_ish""".stripMargin,
        salary = None,
        location = None,
        source = "xorazm_ish",
        url = "https://t.me/Xorazm_ish/123",
        postedAt = postedAt,
        contactLinks = Some(List("https://forms.gle/apply", "https://t.me/Xorazm_ish")),
      )

    val details = XorazmIshSourceJobEtl.enrich(rawJob)

    expect.same(Some("Onlayn anketa"), details.contactText) &&
    expect.same(List("https://forms.gle/apply"), details.contactLinks) &&
    expect.same(List.empty[String], details.contactTelegramUsernames) &&
    expect(details.hasContacts)
  }

  pureTest("marks posts without real contact information as invalid") {
    val rawJob =
      RawJob(
        title = "ishchilar",
        company = Some("Firma"),
        description =
          """#ish
            |Firma ishchilar ishga taklif qilinadi.
            |
            |Talablar:
            |• Mas'uliyatli bo'lishi kerak
            |
            |👉 @Xorazm_ish""".stripMargin,
        salary = None,
        location = None,
        source = "xorazm_ish",
        url = "https://t.me/Xorazm_ish/124",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = XorazmIshSourceJobEtl.enrich(rawJob)

    expect(!details.hasContacts) &&
    expect.same(None, details.contactText) &&
    expect.same(List.empty[String], details.contactPhoneNumbers) &&
    expect.same(List.empty[String], details.contactTelegramUsernames) &&
    expect.same(List.empty[String], details.contactLinks)
  }

  pureTest("extracts responsibilities, benefits, and additional blocks") {
    val rawJob =
      RawJob(
        title = "administrator",
        company = Some("Test"),
        description =
          """#ish
            |Test kompaniyasiga administrator ishga taklif qilinadi.
            |
            |Talablar:
            |• Kompyuterda ishlay olishi
            |
            |Vazifalar:
            |• Mijozlar bilan ishlash
            |• Hisobot yuritish
            |
            |Biz taklif qilamiz:
            |✓ Raqobatbardosh ish haqqi
            |✓ Tushlik ish xona hisobidan
            |✓ Rasmiy ishga kirish
            |
            |Ish vaqti: 09:00 - 18:00
            |✓ Tajribasiga yo'q ammo yaxshi nomzodlarga soha o'rgatiladi
            |
            |Tel: +998901112233
            |👉 @Xorazm_ish""".stripMargin,
        salary = None,
        location = None,
        source = "xorazm_ish",
        url = "https://t.me/Xorazm_ish/125",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = XorazmIshSourceJobEtl.enrich(rawJob)

    expect.same(Some("Mijozlar bilan ishlash\nHisobot yuritish"), details.responsibilities) &&
    expect.same(
      Some("Raqobatbardosh ish haqqi\nTushlik ish xona hisobidan\nRasmiy ishga kirish"),
      details.benefits,
    ) &&
    expect.same(
      Some("Tajribasiga yo'q ammo yaxshi nomzodlarga soha o'rgatiladi"),
      details.additional,
    )
  }

  pureTest("stops requirements before transport and contact sections") {
    val rawJob =
      RawJob(
        title = "Tikuvchi",
        company = Some("Voentorg"),
        description =
          """ISH BOR
            |💼 Ish lavozimi: Tikuvchi
            |
            |🏢 Ish beruvchi: Voentorg
            |
            |📍 Manzil: Urganch shahri, Temir yo'l kasalxonasi yonidagi 4 qavatli uy
            |
            |⏰ Ish vaqti:
            |Haftada 6 kun
            |08:30 - 18:00
            |
            |💰 Ish haqi: 2 000 000 - 6 000 000 so'm
            |(Ish sifati va hajmiga qarab belgilanadi)
            |
            |📝 Talablar:
            |• Tikuvchilik bo'yicha ish tajribasi bo'lishi shart
            |• Faqat ayollar
            |• Chokni tekis va sifatli tikish ko'nikmasi
            |• Urganch shahri aholisi bo'lishi afzal
            |
            |🚌 Transport yo'nalishlari:
            |• 5-avtobus
            |• 4-avtobus
            |• 9-avtobus
            |• 19-damas
            |
            |📞 Bog'lanish:
            |📱 +998 94 524 12 85
            |📱 +998 91 430 43 34
            |
            |👉 @Xorazm_ish_bor_elonlar""".stripMargin,
        salary = Some("2 000 000 - 6 000 000 so'm"),
        location = Some("Urganch shahri, Temir yo'l kasalxonasi yonidagi 4 qavatli uy"),
        source = "xorazm_ish_bor_elonlar",
        url = "https://t.me/Xorazm_ish_bor_elonlar/999999",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = XorazmIshSourceJobEtl.enrich(rawJob)

    expect.same(
      Some(
        "Tikuvchilik bo'yicha ish tajribasi bo'lishi shart\nFaqat ayollar\nChokni tekis va sifatli tikish ko'nikmasi\nUrganch shahri aholisi bo'lishi afzal"
      ),
      details.requirements,
    ) &&
    expect.same(
      Some("Transport yo'nalishlari\n5-avtobus\n4-avtobus\n9-avtobus\n19-damas"),
      details.additional,
    ) &&
    expect.same(None, details.contactText) &&
    expect.same(List("+998945241285", "+998914304334"), details.contactPhoneNumbers)
  }

  pureTest("moves eslatma block out of requirements") {
    val rawJob =
      RawJob(
        title = "Sotuvchi-konsultant",
        company = Some("SANTEXPARK Gipermarket (Qurilish va santexnika mahsulotlari)"),
        description =
          """📣📣📣📣
            |💼 Ish lavozimi: Sotuvchi-konsultant
            |
            |🏢 Ish beruvchi: SANTEXPARK Gipermarket (Qurilish va santexnika mahsulotlari)
            |
            |📍 Manzil: Hamid Olimjon 226
            |🎯 Mo'ljal: Ekskavator zavodi yonida
            |
            |⏰ Ish vaqti:
            |08:30 – 18:00
            |
            |💰 Ish haqi: Suhbat asosida kelishiladi
            |
            |📝 Talablar:
            |• Qizlar
            |• Yosh: 20–30
            |• Xushmuomala va chaqqon bo'lishi
            |• Ishga mas'uliyat bilan yondashish
            |• Ichki tartib-qoidalarga rioya qilish
            |• Avval shu sohada ishlagan bo'lsa ustunlik beriladi
            |
            |📌 Eslatma:
            |• Urganch yoki G'oybu mahallasida yashovchilar murojaat qilsin
            |• Talabalar qabul qilinmaydi
            |• Uzoqdan keladiganlar bezovta qilmasin
            |
            |📞 Bog'lanish:
            |📱 +998 91 868 22 19
            |📱 +998 91 868 22 10
            |
            |👉 @Xorazm_ish_bor_elonlar""".stripMargin,
        salary = Some("Suhbat asosida kelishiladi"),
        location = Some("Hamid Olimjon 226 (Ekskavator zavodi yonida)"),
        source = "xorazm_ish_bor_elonlar",
        url = "https://t.me/Xorazm_ish_bor_elonlar/999998",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = XorazmIshSourceJobEtl.enrich(rawJob)

    expect.same(
      Some(
        "Qizlar\nYosh: 20–30\nXushmuomala va chaqqon bo'lishi\nIshga mas'uliyat bilan yondashish\nIchki tartib-qoidalarga rioya qilish\nAvval shu sohada ishlagan bo'lsa ustunlik beriladi"
      ),
      details.requirements,
    ) &&
    expect.same(
      Some(
        "Eslatma\nUrganch yoki G'oybu mahallasida yashovchilar murojaat qilsin\nTalabalar qabul qilinmaydi\nUzoqdan keladiganlar bezovta qilmasin"
      ),
      details.additional,
    ) &&
    expect.same(None, details.contactText) &&
    expect.same(List("+998918682219", "+998918682210"), details.contactPhoneNumbers)
  }

  pureTest("keeps salary continuation and contact cta out of additional") {
    val rawJob =
      RawJob(
        title = "Gold Restoranga Tayyorlab biladigan povrlarga ish bor",
        company = Some("Gold Restoran"),
        description =
          """YANGI ISH
            |***YANGI***Gold Restoranga
            |• POVR PROFESSIONAL 7-10
            |• Turetski kuhnya
            |• Tandirda Lavash avgan non
            |• Gosht steak
            |• Barista cofe maxito koktell
            |• Salat opitni
            |• Desert
            |Tayyorlab biladigan povrlarga ish bor ✅
            |
            |Paspurt
            |Med qnishka
            |Yashash joyi Urganch shahar
            |
            |🧾YOSHI: 20 Dan 35 gacha
            |📋MA'LUMOTI:
            |👨‍🍳👩‍🍳JINSI: Ayol Erkak
            |
            |⏰ISH VAQTI: yangi ochiladi nomalum
            |💰 OYLIK MAOSH:
            |POVR 150-300 som
            |Opitina qorob belgilanadi
            |
            |📍 MANZIL: GOLD Resto bar
            |🎯MO'LJAL: oblasnoy Raddom
            |
            |📌 AGAR TALABLAR SIZGA MAQUL KELGAN BO'LSA BIZ BILAN BOG'LANING!
            |
            |📞 Bog'lanish 👇
            |📞 Telefon raqam:
            |+998995277775
            |
            |👉 @Xorazm_ish_bor_elonlar""".stripMargin,
        salary = Some("150-300 som"),
        location = Some("GOLD Resto bar (oblasnoy Raddom)"),
        source = "xorazm_ish_bor_elonlar",
        url = "https://t.me/Xorazm_ish_bor_elonlar/999997",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = XorazmIshSourceJobEtl.enrich(rawJob)
    val additional = details.additional.getOrElse("")

    expect(!additional.contains("POVR 150-300 som")) &&
    expect(!additional.contains("Opitina qorob belgilanadi")) &&
    expect(!additional.contains("AGAR TALABLAR")) &&
    expect(!additional.contains("Telefon raqam")) &&
    expect(!additional.contains("+998995277775")) &&
    expect.same(List("+998995277775"), details.contactPhoneNumbers)
  }

  pureTest("keeps only real schedule lines in work schedule") {
    val rawJob =
      RawJob(
        title = "operator",
        company = Some("Test"),
        description =
          """#ish
            |Kompaniyaga operatorlar ishga taklif qilinadi.
            |
            |Ish vaqti: To'liq stavka ish
            |09:00 - 14:00 (1-smena)
            |14:00 - 22:30 (2-smena)
            |
            |Oylik suhbat asosida kelishiladi
            |Qo'shimcha ma'lumotlar uchun hoziroq pastdagi havolani bosib anketani to'ldiring sizga o'zimiz bog'lanamiz
            |USTIGA BOSING
            |http://zokadr.uz/finliteishanketa
            |
            |Tel: +998901112233
            |👉 @Xorazm_ish""".stripMargin,
        salary = None,
        location = None,
        source = "xorazm_ish",
        url = "https://t.me/Xorazm_ish/128",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = XorazmIshSourceJobEtl.enrich(rawJob)

    expect.same(
      Some("To'liq stavka ish\n09:00 - 14:00 (1-smena)\n14:00 - 22:30 (2-smena)"),
      details.workSchedule,
    )
  }

  pureTest("does not leak requirement lines into contact text") {
    val rawJob =
      RawJob(
        title = "xostes",
        company = Some("Fit Leader"),
        description =
          """#ish
            |''Fit Leader'' fitness klubiga sotuv menejer - xostes ishga taklif qilinadi.
            |
            |Talablar:
            |• Telefon qo'ng'iroqlariga javob bera olishi
            |• Xushmuomala va mas'uliyatli bo'lishi kerak
            |
            |Tel: +998958684646
            |
            |👉 @Xorazm_ish""".stripMargin,
        salary = None,
        location = None,
        source = "xorazm_ish",
        url = "https://t.me/Xorazm_ish/129",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = XorazmIshSourceJobEtl.enrich(rawJob)

    expect.same(None, details.contactText)
  }

  pureTest("keeps resume instruction text clean after removing telegram username") {
    val rawJob =
      RawJob(
        title = "operator",
        company = Some("Ostonadent 3D"),
        description =
          """#ish
            |''Ostonadent 3D'' markaziga operatorlar ishga taklif qilinadi.
            |
            |Murojaat uchun:
            |Rezyumengizni telegram orqali @ostona3bot ga 1 ta xabar ko'rinishida yuboring.
            |
            |👉 @Xorazm_ish""".stripMargin,
        salary = None,
        location = None,
        source = "xorazm_ish",
        url = "https://t.me/Xorazm_ish/130",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = XorazmIshSourceJobEtl.enrich(rawJob)

    expect.same(
      Some("Rezyumengizni telegram orqali 1 ta xabar ko'rinishida yuboring"),
      details.contactText,
    ) &&
    expect.same(List("ostona3bot"), details.contactTelegramUsernames)
  }

  pureTest("normalizes title into sentence case without breaking acronyms") {
    val details =
      JobDetails(
        requirements = None,
        responsibilities = None,
        benefits = None,
        additional = None,
        workSchedule = None,
        contactText = None,
        contactPhoneNumbers = List("+998901112233"),
        contactTelegramUsernames = List.empty,
        contactLinks = List.empty,
      )

    val lowerTitle =
      dto.Job.fromEvent(
        input =
          RawJob(
            title = "ishchilar",
            company = Some("Test"),
            description = "text",
            salary = None,
            location = None,
            source = "xorazm_ish",
            url = "https://t.me/Xorazm_ish/126",
            postedAt = postedAt,
            contactLinks = None,
          ),
        id = UUID.fromString("00000000-0000-0000-0000-000000000126"),
        createdAt = postedAt,
        details = details,
      )

    val acronymTitle =
      dto.Job.fromEvent(
        input =
          RawJob(
            title = "AQEM",
            company = Some("Test"),
            description = "text",
            salary = None,
            location = None,
            source = "xorazm_ish",
            url = "https://t.me/Xorazm_ish/127",
            postedAt = postedAt,
            contactLinks = None,
          ),
        id = UUID.fromString("00000000-0000-0000-0000-000000000127"),
        createdAt = postedAt,
        details = details,
      )

    expect.same("Ishchilar", lowerTitle.title) &&
    expect.same("AQEM", acronymTitle.title)
  }
}
