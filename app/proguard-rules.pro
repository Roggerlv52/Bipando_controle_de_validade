# ✅ Preserva os modelos de dados usados pelo Firestore e pelo Room
# (Impede o R8 de renomear os campos de reflexão dos seus produtos e categorias)
-keep class com.rogger.bp.data.model.** { *; }

# Mantém os metadados e anotações do Firebase/Firestore
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Mantém as classes internas do Firebase
-keep class com.google.firebase.** { *; }

# Mantém as classes do Google Play Billing (Faturação Premium)
-keep class com.android.billingclient.** { *; }