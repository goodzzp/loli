package org.loli.html

/**
 * css相关
 * @author goodzhu
 */
internal object CssUtil {
    /**
     * 表格的css
     */
    val CSS_TABLE = """
                <style type="text/css">
                .customers
                  {
                      font-family:"Trebuchet MS", Arial, Helvetica, sans-serif;
                      width: 90%;
                      border-collapse:collapse;
                  }

                .customers td, .customers th
                  {
                      font-size:1em;
                      border:1px solid #98bf21;
                      padding:3px 7px 2px 7px;
                  }

                .customers th
                  {
                      font-size:1.1em;
                      text-align:left;
                      padding-top:5px;
                      padding-bottom:4px;
                      background-color:#558833;
                      color:#ffffff;
                  }

                .customers tr.alt td
                  {
                      color:#000000;
                      background-color:#EAF2D3;
                  }

                .high_light
                {
                    background-color:#f8ffd9;
                }

                .tbl_method_desc
                {
                    border-spacing: 0;
                    width: 100%;
                }

                .tbl_method_desc td, .tbl_method_desc th
                {
                        border-color: #ffffff;
                }

                td .td_name
                {
                        font-weight: bold;
                        width: 20%;
                }
                </style>
    """.trimIndent()
}
