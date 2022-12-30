package org.cruldra.pdmviewer.utils

import org.cruldra.pdmviewer.models.PDMColumn
import org.cruldra.pdmviewer.parser.PDMParseException
import java.util.*

fun PDMColumn.getIbatisType():String{
    return  when(val shortName= this.dataType.substringBefore("(")){
        "varchar" , "double"-> shortName.uppercase(Locale.getDefault())
        "datetime" -> "TIMESTAMP"
      "int" -> "INTEGER"
      else -> throw  PDMParseException("ibatis没有匹配的数据类型 $shortName")
  }
}
