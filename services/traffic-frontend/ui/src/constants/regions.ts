export interface RegionDefinition {
  code: string
  name: string
}

export const ALL_REGION_CODE = 'ALL'
export const DEFAULT_REGION_CODE = 'SEOUL'

export const REGION_DEFINITIONS: RegionDefinition[] = [
  { code: 'SEOUL', name: '서울' },
  { code: 'BUSAN', name: '부산' },
  { code: 'DAEGU', name: '대구' },
  { code: 'INCHEON', name: '인천' },
  { code: 'GWANGJU', name: '광주' },
  { code: 'DAEJEON', name: '대전' },
  { code: 'ULSAN', name: '울산' },
  { code: 'SEJONG', name: '세종' },
  { code: 'GYEONGGI', name: '경기' },
  { code: 'GANGWON', name: '강원' },
  { code: 'CHUNGBUK', name: '충북' },
  { code: 'CHUNGNAM', name: '충남' },
  { code: 'JEONBUK', name: '전북' },
  { code: 'JEONNAM', name: '전남' },
  { code: 'GYEONGBUK', name: '경북' },
  { code: 'GYEONGNAM', name: '경남' },
  { code: 'JEJU', name: '제주' },
]

const regionNameByCode = new Map(REGION_DEFINITIONS.map((region) => [region.code, region.name]))

export function formatRegionLabel(regionCode: string): string {
  const normalized = regionCode.trim().toUpperCase()
  if (normalized === ALL_REGION_CODE) {
    return '전체 (ALL)'
  }

  const knownName = regionNameByCode.get(normalized)
  if (knownName) {
    return `${knownName} (${normalized})`
  }

  return normalized
}

export function mergeRegionCodes(...groups: string[][]): string[] {
  const merged = new Set<string>()

  groups.forEach((group) => {
    group.forEach((regionCode) => {
      const normalized = regionCode.trim().toUpperCase()
      if (normalized.length > 0) {
        merged.add(normalized)
      }
    })
  })

  return [...merged]
}
