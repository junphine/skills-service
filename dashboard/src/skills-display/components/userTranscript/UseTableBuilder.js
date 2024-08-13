/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export const useTableBuilder = () => {
  const lineGap = 5
  const lightGray = '#f5f9ff'
  const darkBlue = '#295bac'
  const arrowColor1 = '#264653'

  const addRectangle = (doc, parentSection, isHeader = false) => {
    const rowBackgroundStruct = doc.struct('Artifact', { type: 'Layout' }, () => {
      doc.fillColor(isHeader ? darkBlue : lightGray)
      const lineHeight = doc.currentLineHeight() + lineGap

      const startX = 47
      const totalWidth = doc.page.width - (startX * 2)

      doc.rect(startX, doc.y - lineGap, totalWidth, lineHeight + 2)
      doc.fill()
      doc.fillColor(isHeader ? 'white' : arrowColor1)
    })
    parentSection.add(rowBackgroundStruct)
  }

  const addTable = (doc, sectionToAddTo, tableInfo) => {
    const startX = 50
    const totalWidth = doc.page.width - startX
    const columnWidth = totalWidth / tableInfo.headers.length

    doc.lineGap(lineGap)

    const headersWrapped = tableInfo.headers.map((header, index) => {
      return {
        value: header, x: (startX + (index * columnWidth))
      }
    })
    const rowsWrapped = tableInfo.rows.map((row) => {
      return row.map((cell, index) => {
        const column = tableInfo.headers[index]
        return {
          value: cell, column, x: headersWrapped[index].x
        }
      })
    })

    const table = doc.struct('Table', { title: tableInfo.title })
    sectionToAddTo.add(table)

    const headerRowConstruct = doc.struct('TR', { title: `Header Row` })
    table.add(headerRowConstruct)
    addRectangle(doc, headerRowConstruct, true)
    headersWrapped.forEach((cell, cellIndex) => {
      if (cellIndex > 0) {
        doc.moveUp()
      }
      const cellStruct = doc.struct('TH', { title: `${cell.column} Column Header` }, () => {
        doc.text(cell.value, cell.x)
      })
      headerRowConstruct.add(cellStruct)
    })

    rowsWrapped.forEach((row, rowIndex) => {
      const rowStruct = doc.struct('TR', { title: `Row ${rowIndex}` })
      table.add(rowStruct)
      if (rowIndex === 0 || rowIndex % 2 === 0) {
        addRectangle(doc, rowStruct)
      }

      row.forEach((cell, cellIndex) => {
        if (cellIndex > 0) {
          doc.moveUp()
        }
        const cellStruct = doc.struct('TD', { title: `Row ${rowIndex} ${cell.column} Column` }, () => {
          if (cell.value instanceof Object) {
            const actualValue = cell.value.value
            const link = cell.value.link
            doc.text(actualValue, cell.x, null, { link })
          } else {
            doc.text(cell.value, cell.x)
          }
        })
        rowStruct.add(cellStruct)
      })


    })

    return table
  }

  return {
    addTable
  }
}