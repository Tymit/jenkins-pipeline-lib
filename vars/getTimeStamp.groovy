def String call() {
   Date date = new Date()
   return date.format('yyyyMMddHHmmss',TimeZone.getTimeZone('CEST')) as String
}
return this
