package org.egov.edcr.constants;

public enum OdishaUlb {
	CUTTACK("od.cuttack", "Cuttack Municipal Corporation");

	private String ulbCode;
	private String ulbName;

	private OdishaUlb(String ulbCode, String ulbName) {
		this.ulbCode = ulbCode;
		this.ulbName = ulbName;
	}

	public String getUlbCode() {
		return ulbCode;
	}

	public String getUlbName() {
		return ulbName;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return getUlbCode() + " - " + getUlbName();
	}
}
