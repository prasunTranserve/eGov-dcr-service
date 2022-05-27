package org.egov.edcr.constants;

public enum OdishaUlbs {
	CUTTACK("od.cuttack", "Cuttack Municipal Corporation");

	private String ulbCode;
	private String ulbName;

	private OdishaUlbs(String ulbCode, String ulbName) {
		this.ulbCode = ulbCode;
		this.ulbName = ulbName;
	}

	public String getUlbCode() {
		return ulbCode;
	}

	public String getUlbName() {
		return ulbName;
	}

	public static OdishaUlbs getUlb(String ulbCode) {
		for (OdishaUlbs v : values()) {
			if (v.ulbCode.equals(ulbCode)) {
				return v;
			}
		}
		return valueOf(ulbCode);
	}

	@Override
	public String toString() {
		return getUlbCode() + " - " + getUlbName();
	}
}
