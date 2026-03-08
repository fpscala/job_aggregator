package uz.scala.shared

import uz.scala.Language
import uz.scala.Language._

object ResponseMessages {
  val FILE_NOT_FOUND: Map[Language, String] = Map(
    En -> "File not found",
    Ru -> "Файл не найден",
    Uz -> "Fayl topilmadi",
  )

  val FILE_CREATED: Map[Language, String] = Map(
    En -> "File successfully created",
    Ru -> "Файл успешно создан",
    Uz -> "Fayl yaratildi",
  )

  val USER_NOT_FOUND: Map[Language, String] = Map(
    En -> "User not found",
    Ru -> "Пользователь не найден",
    Uz -> "Foydalanuvchi topilmadi",
  )

  val PHONE_Y_EXISTS: Map[Language, String] = Map(
    En -> "This phone number already exists",
    Ru -> "Этот номер телефона уже существует",
    Uz -> "Ushbu telefon raqam allaqachon mavjud",
  )

  val USER_CREATED: Map[Language, String] = Map(
    En -> "User successfully created",
    Ru -> "Пользователь успешно создан",
    Uz -> "Foydalanuvchi yaratildi",
  )

  val USER_UPDATED: Map[Language, String] = Map(
    En -> "User updated",
    Ru -> "Пользователь обновлен",
    Uz -> "Foydalanuvchi yangilandi",
  )

  val USER_DELETED: Map[Language, String] = Map(
    En -> "User deleted",
    Ru -> "Пользователь удален",
    Uz -> "Foydalanuvchi o'chirildi",
  )

  val PASSWORD_UPDATED: Map[Language, String] = Map(
    En -> "Password updated",
    Ru -> "Пароль обновлен",
    Uz -> "Parol yangilandi",
  )

  val WRONG_PASSWORD: Map[Language, String] = Map(
    En -> "Wrong password",
    Ru -> "Неверный пароль",
    Uz -> "Noto'g'ri parol",
  )

  val PRIVILEGE_CREATE_USER: Map[Language, String] = Map(
    En -> "You have no privileges to create user",
    Ru -> "У вас нет привилегий создавать пользователя",
    Uz -> "Foydalanuvchi yaratish uchun ruxsat yo'q",
  )

  val CREATE_SUPER_USER: Map[Language, String] = Map(
    En -> "Can't create super user",
    Ru -> "Нельзя создавать суперпользователя",
    Uz -> "Super foydalanuvchi yaratish uchun ruxsat yo'q",
  )

  val PRIVILEGE_CREATE_SUPER_USER: Map[Language, String] = Map(
    En -> "You have no privileges to create super user",
    Ru -> "У вас нет привилегий создавать суперпользователя",
    Uz -> "Sizda super foydalanuvchi yaratish uchun ruxsat yo'q",
  )

  val PASSWORD_DOES_NOT_MATCH: Map[Language, String] = Map(
    En -> "Sms code does not match",
    Ru -> "Код подтверждения не совпадает",
    Uz -> "SMS kodi mos kelmadi",
  )

  val INSUFFICIENT_PRIVILEGES: Map[Language, String] = Map(
    En -> "Forbidden. Insufficient privileges",
    Ru -> "Запрещено. Недостаточно привилегий",
    Uz -> "Taqiqlangan. Imtiyozlar yetarli emas",
  )

  val AUTHENTICATION_REQUIRED: Map[Language, String] = Map(
    En -> "Authentication required",
    Ru -> "Требуется аутентификация",
    Uz -> "Autentifikatsiya talab qilinadi",
  )

  val INVALID_TOKEN: Map[Language, String] = Map(
    En -> "Invalid token or expired",
    Ru -> "Неверный токен или токен устарел",
    Uz -> "Yaroqsiz yoki eskirgan token",
  )

  val BEARER_TOKEN_NOT_FOUND: Map[Language, String] = Map(
    En -> "Bearer token not found",
    Ru -> "Токен не найден",
    Uz -> "Bearer token topilmadi",
  )

  val ROLE_NOT_FOUND: Map[Language, String] = Map(
    En -> "Role not found",
    Ru -> "Роль не найдена",
    Uz -> "Rol topilmadi",
  )

  val ROLE_CREATED: Map[Language, String] = Map(
    En -> "Role successfully created",
    Ru -> "Роль успешно создана",
    Uz -> "Rol yaratildi",
  )

  val ROLE_UPDATED: Map[Language, String] = Map(
    En -> "Role successfully updated",
    Ru -> "Роль успешно обновлена",
    Uz -> "Rol yangilandi",
  )

  val ROLE_DELETED: Map[Language, String] = Map(
    En -> "Role successfully deleted",
    Ru -> "Роль успешно удалена",
    Uz -> "Rol o'chirildi",
  )

  val SALARY_RANGE_INVALID: Map[Language, String] = Map(
    En -> "Salary range is invalid: minimum salary cannot be higher than maximum salary",
    Ru -> "Диапазон зарплат недействителен: минимальная зарплата не может быть выше максимальной",
    Uz -> "Maosh diapazoni noto'g'ri: minimal maosh maksimaldan yuqori bo'lishi mumkin emas",
  )

  val APPLICATION_DEADLINE_PAST: Map[Language, String] = Map(
    En -> "Application deadline cannot be in the past",
    Ru -> "Срок подачи заявок не может быть в прошлом",
    Uz -> "Ariza berish muddati o'tmishda bo'lishi mumkin emas",
  )

  val INVALID_JOB_DATA: Map[Language, String] = Map(
    En -> "Job must have both requirements and skills specified",
    Ru -> "У вакансии должны быть указаны как требования, так и навыки",
    Uz -> "Ish o'rni talablar va ko'nikmalar bilan ko'rsatilgan bo'lishi kerak",
  )

  val JOB_NOT_FOUND: Map[Language, String] = Map(
    En -> "Job not found",
    Ru -> "Вакансия не найдена",
    Uz -> "Ish o'rni topilmadi",
  )

  val JOB_CREATED: Map[Language, String] = Map(
    En -> "Job successfully created",
    Ru -> "Вакансия успешно создана",
    Uz -> "Ish o'rni yaratildi",
  )

  val JOB_UPDATED: Map[Language, String] = Map(
    En -> "Job successfully updated",
    Ru -> "Вакансия успешно обновлена",
    Uz -> "Ish o'rni yangilandi",
  )

  val JOB_DELETED: Map[Language, String] = Map(
    En -> "Job successfully deleted",
    Ru -> "Вакансия успешно удалена",
    Uz -> "Ish o'rni o'chirildi",
  )

  val JOB_PUBLISHED: Map[Language, String] = Map(
    En -> "Job successfully published",
    Ru -> "Вакансия успешно опубликована",
    Uz -> "Ish o'rni e'lon qilindi",
  )

  val JOB_PAUSED: Map[Language, String] = Map(
    En -> "Job successfully paused",
    Ru -> "Вакансия приостановлена",
    Uz -> "Ish o'rni to'xtatildi",
  )

  val JOB_CLOSED: Map[Language, String] = Map(
    En -> "Job successfully closed",
    Ru -> "Вакансия закрыта",
    Uz -> "Ish o'rni yopildi",
  )

  val JOB_EXPIRED: Map[Language, String] = Map(
    En -> "Job has expired",
    Ru -> "Срок вакансии истек",
    Uz -> "Ish o'rni muddati o'tgan",
  )

  val COMPANY_NOT_FOUND: Map[Language, String] = Map(
    En -> "Company not found",
    Ru -> "Компания не найдена",
    Uz -> "Kompaniya topilmadi",
  )

  val CATEGORY_NOT_FOUND: Map[Language, String] = Map(
    En -> "Job category not found",
    Ru -> "Категория вакансий не найдена",
    Uz -> "Ish kategoriyasi topilmadi",
  )

  val INVALID_JOB_STATUS_TRANSITION: Map[Language, String] = Map(
    En -> "Invalid job status transition",
    Ru -> "Недопустимый переход статуса вакансии",
    Uz -> "Ish o'rni holati o'zgarishi noto'g'ri",
  )

  val JOB_CANNOT_BE_PUBLISHED: Map[Language, String] = Map(
    En -> "Job cannot be published due to missing required fields",
    Ru -> "Вакансия не может быть опубликована из-за отсутствующих обязательных полей",
    Uz -> "Ish o'rni majburiy maydonlar yo'qligi sababli e'lon qilinmaydi",
  )

  val JOB_ACCESS_DENIED: Map[Language, String] = Map(
    En -> "You do not have permission to access this job",
    Ru -> "У вас нет разрешения на доступ к этой вакансии",
    Uz -> "Bu ish o'rniga kirish huquqingiz yo'q",
  )

  val JOB_MODIFICATION_DENIED: Map[Language, String] = Map(
    En -> "You do not have permission to modify this job",
    Ru -> "У вас нет разрешения на изменение этой вакансии",
    Uz -> "Bu ish o'rnini o'zgartirish huquqingiz yo'q",
  )

  val JOB_TITLE_REQUIRED: Map[Language, String] = Map(
    En -> "Job title is required",
    Ru -> "Название вакансии обязательно",
    Uz -> "Ish o'rni nomi majburiy",
  )

  val JOB_DESCRIPTION_REQUIRED: Map[Language, String] = Map(
    En -> "Job description is required",
    Ru -> "Описание вакансии обязательно",
    Uz -> "Ish o'rni tavsifi majburiy",
  )

  val JOB_CATEGORY_REQUIRED: Map[Language, String] = Map(
    En -> "Job category is required",
    Ru -> "Категория вакансии обязательна",
    Uz -> "Ish o'rni kategoriyasi majburiy",
  )

  val JOB_CONTACT_INFO_REQUIRED: Map[Language, String] = Map(
    En -> "At least one contact method (email or application URL) is required",
    Ru -> "Требуется хотя бы один способ связи (email или URL для подачи заявки)",
    Uz -> "Kamida bitta aloqa usuli (email yoki ariza URL) talab qilinadi",
  )

  val SKILLS_NOT_FOUND: Map[Language, String] = Map(
    En -> "Some specified skills do not exist",
    Ru -> "Некоторые указанные навыки не существуют",
    Uz -> "Ba'zi ko'rsatilgan ko'nikmalar mavjud emas",
  )

  val EMAIL_ALREADY_EXISTS: Map[Language, String] = Map(
    En -> "Email already exists",
    Ru -> "Email уже зарегистрирован",
    Uz -> "Bu email allaqachon ro'yxatdan o'tgan",
  )

  val REGISTRATION_SUCCESS: Map[Language, String] = Map(
    En -> "Registration successful. Activation link sent to your email",
    Ru -> "Регистрация успешна. Ссылка активации отправлена на ваш email",
    Uz -> "Ro'yxatdan o'tdingiz. Email manzilingizga aktivatsiya havolasi yuborildi",
  )

  val INVALID_ACTIVATION_TOKEN: Map[Language, String] = Map(
    En -> "Invalid or expired activation token",
    Ru -> "Неверный или истекший токен активации",
    Uz -> "Noto'g'ri yoki muddati o'tgan aktivatsiya tokeni",
  )

  val ACTIVATION_SUCCESS: Map[Language, String] = Map(
    En -> "Account activated successfully",
    Ru -> "Аккаунт успешно активирован",
    Uz -> "Hisobingiz muvaffaqiyatli faollashtirildi",
  )

  val ACCOUNT_NOT_ACTIVE: Map[Language, String] = Map(
    En -> "Account is not active",
    Ru -> "Аккаунт не активен",
    Uz -> "Hisob faol emas",
  )

  val ACCOUNT_PENDING_VERIFICATION: Map[Language, String] = Map(
    En -> "Account pending email verification. Please check your email",
    Ru -> "Аккаунт ожидает подтверждения email. Пожалуйста, проверьте вашу почту",
    Uz -> "Hisob email tasdiqlanishini kutmoqda. Emailingizni tekshiring",
  )

  val ACCOUNT_SUSPENDED: Map[Language, String] = Map(
    En -> "Account has been suspended. Please contact support",
    Ru -> "Аккаунт заблокирован. Пожалуйста, свяжитесь с поддержкой",
    Uz -> "Hisob bloklangan. Qo'llab-quvvatlash xizmatiga murojaat qiling",
  )

  // Draft messages
  val DRAFT_NOT_FOUND: Map[Language, String] = Map(
    En -> "Job draft not found",
    Ru -> "Черновик вакансии не найден",
    Uz -> "Ish o'rni qoralama topilmadi",
  )

  val DRAFT_CANNOT_BE_EDITED: Map[Language, String] = Map(
    En -> "Draft cannot be edited in current status",
    Ru -> "Черновик не может быть отредактирован в текущем статусе",
    Uz -> "Qoralama hozirgi holatda tahrir qilinishi mumkin emas",
  )

  val DRAFT_NOT_READY_FOR_SUBMISSION: Map[Language, String] = Map(
    En -> "Draft is not ready for submission. Complete all required steps first",
    Ru -> "Черновик не готов к отправке. Сначала завершите все обязательные шаги",
    Uz -> "Qoralama taqdim etishga tayyor emas. Avval barcha majburiy qadamlarni bajaring",
  )

  val DRAFT_ALREADY_SUBMITTED: Map[Language, String] = Map(
    En -> "Draft has already been submitted",
    Ru -> "Черновик уже отправлен",
    Uz -> "Qoralama allaqachon taqdim etilgan",
  )

  val DRAFT_CANNOT_BE_REVIEWED: Map[Language, String] = Map(
    En -> "Draft is not available for review",
    Ru -> "Черновик недоступен для проверки",
    Uz -> "Qoralama ko'rib chiqish uchun mavjud emas",
  )

  val DRAFT_CANNOT_BE_WITHDRAWN: Map[Language, String] = Map(
    En -> "Only drafts under review can be withdrawn",
    Ru -> "Только черновики на проверке могут быть отозваны",
    Uz -> "Faqat ko'rib chiqilayotgan qoralamalar qaytarib olinishi mumkin",
  )

  val DRAFT_NOT_APPROVED: Map[Language, String] = Map(
    En -> "Only approved drafts can be processed",
    Ru -> "Только одобренные черновики могут быть обработаны",
    Uz -> "Faqat tasdiqlangan qoralamalar qayta ishlanishi mumkin",
  )

  val DRAFT_ALREADY_PROCESSED: Map[Language, String] = Map(
    En -> "Draft has already been processed",
    Ru -> "Черновик уже обработан",
    Uz -> "Qoralama allaqachon qayta ishlangan",
  )

  val DRAFT_NOT_OWNED_BY_USER: Map[Language, String] = Map(
    En -> "You do not have permission to access this draft",
    Ru -> "У вас нет доступа к этому черновику",
    Uz -> "Sizda bu qoralamaga kirish huquqi yo'q",
  )

  val DRAFT_CREATED: Map[Language, String] = Map(
    En -> "Draft successfully created",
    Ru -> "Черновик успешно создан",
    Uz -> "Qoralama yaratildi",
  )

  val DRAFT_UPDATED: Map[Language, String] = Map(
    En -> "Draft successfully updated",
    Ru -> "Черновик успешно обновлен",
    Uz -> "Qoralama yangilandi",
  )

  val DRAFT_SUBMITTED_FOR_REVIEW: Map[Language, String] = Map(
    En -> "Draft submitted for review",
    Ru -> "Черновик отправлен на проверку",
    Uz -> "Qoralama ko'rib chiqish uchun yuborildi",
  )

  val DRAFT_APPROVED: Map[Language, String] = Map(
    En -> "Draft approved",
    Ru -> "Черновик одобрен",
    Uz -> "Qoralama tasdiqlandi",
  )

  val DRAFT_REJECTED: Map[Language, String] = Map(
    En -> "Draft rejected",
    Ru -> "Черновик отклонен",
    Uz -> "Qoralama rad etildi",
  )

  val DRAFT_WITHDRAWN: Map[Language, String] = Map(
    En -> "Draft withdrawn from review",
    Ru -> "Черновик отозван с проверки",
    Uz -> "Qoralama ko'rib chiqishdan qaytarib olindi",
  )

  val DRAFT_PROCESSED: Map[Language, String] = Map(
    En -> "Draft successfully processed",
    Ru -> "Черновик успешно обработан",
    Uz -> "Qoralama qayta ishlandi",
  )

  val DRAFT_DELETED: Map[Language, String] = Map(
    En -> "Draft successfully deleted",
    Ru -> "Черновик успешно удален",
    Uz -> "Qoralama o'chirildi",
  )

  val INVALID_DRAFT_STATUS: Map[Language, String] = Map(
    En -> "Invalid draft status",
    Ru -> "Недопустимый статус черновика",
    Uz -> "Noto'g'ri qoralama holati",
  )

  val DRAFT_CONTENT_INVALID: Map[Language, String] = Map(
    En -> "Draft content is invalid or incomplete",
    Ru -> "Содержимое черновика недействительно или неполно",
    Uz -> "Qoralama mazmuni noto'g'ri yoki to'liq emas",
  )
}
